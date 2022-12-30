package tastymima

import scala.annotation.tailrec

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Flags.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Types.*

import Problems.*
import Utils.*

private[tastymima] final class Analyzer(val oldCtx: Context, val newCtx: Context):
  import Analyzer.*

  private val _problems = mutable.ListBuffer.empty[Problem]

  def analyzeTopSymbols(oldTopSymbols: List[Symbol], newTopSymbols: List[Symbol]): Unit =
    for topSymbol <- oldTopSymbols do
      topSymbol match
        case topClass: ClassSymbol => analyzeTopClass(topClass)
        case _                     => ()
  end analyzeTopSymbols

  def allProblems: List[Problem] =
    _problems.toList

  private def reportProblem(problem: Problem): Unit =
    _problems += problem

  private def analyzeTopClass(oldTopClass: ClassSymbol): Unit =
    val oldVisibility = symVisibility(oldTopClass)(using oldCtx)
    val isTopAccessible = oldVisibility != Visibility.Private && oldVisibility != Visibility.Protected

    if !isTopAccessible then () // OK
    else
      val path = oldTopClass.fullName.path
      invalidStructureToOption(newCtx.findSymbolFromRoot(path).asClass) match
        case None =>
          reportProblem(Problem.MissingClass(ClassInfo(path)))
        case Some(newTopClass) =>
          analyzeClass(oldTopClass, newTopClass)
  end analyzeTopClass

  private def analyzeClass(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    checkVisibility(oldClass, newClass)

    if oldClass.typeParams(using oldCtx).sizeCompare(newClass.typeParams(using newCtx)) != 0 then
      reportProblem(Problem.TypeArgumentCountMismatch(classInfo(oldClass)(using oldCtx)))
      return // things can severely break further down, in that case

    checkOpenLevel(oldClass, newClass)

    val oldThisType = classThisType(oldClass)(using oldCtx)
    val newThisType = classThisType(newClass)(using newCtx)

    val openBoundary = classOpenBoundary(oldClass)(using oldCtx)

    for oldDecl <- oldClass.declarations(using oldCtx) do
      val oldVisibility = symVisibility(oldDecl)(using oldCtx)
      val isMemberAccessible = oldVisibility match
        case Visibility.Private   => false
        case Visibility.Protected => openBoundary.nonEmpty
        case _                    => true

      if !isMemberAccessible then () // OK
      else
        def oldIsOverridable = memberIsOverridable(oldDecl, openBoundary)(using oldCtx)

        oldDecl match
          case oldDecl: ClassSymbol =>
            newClass.getDecl(oldDecl.name)(using newCtx) match
              case None =>
                reportProblem(Problem.MissingClass(classInfo(oldDecl)(using oldCtx)))
              case Some(newDecl: ClassSymbol) =>
                analyzeClass(oldDecl, newDecl)
              case Some(newDecl: TypeSymbolWithBounds) =>
                reportIncompatibleKindChange(oldDecl, newDecl)

          case oldDecl: TypeMemberSymbol =>
            memberNotFoundToOption(newThisType.member(oldDecl.name)(using newCtx)) match
              case None =>
                reportProblem(Problem.MissingTypeMember(symInfo(oldDecl)(using oldCtx)))
              case Some(newDecl: TypeMemberSymbol) =>
                analyzeTypeMember(oldThisType, oldDecl, oldIsOverridable, newThisType, newDecl)
              case Some(newDecl) =>
                reportIncompatibleKindChange(oldDecl, newDecl.asInstanceOf[TermOrTypeSymbol])

          case _: TypeParamSymbol =>
            () // nothing to do

          case oldDecl: TermSymbol =>
            val signedName = withOldCtx {
              if oldDecl.is(Method) && oldDecl.declaredType.isInstanceOf[ExprType] then oldDecl.name
              else oldDecl.signedName
            }
            memberNotFoundToOption(newThisType.member(signedName)(using newCtx)) match
              case None =>
                reportProblem(Problem.MissingTermMember(symInfo(oldDecl)(using oldCtx)))
              case Some(newDecl) =>
                analyzeTermMember(oldThisType, oldDecl, oldIsOverridable, newThisType, newDecl.asInstanceOf[TermSymbol])
  end analyzeClass

  private def checkOpenLevel(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    val oldOpenLevel = classOpenLevel(oldClass)(using oldCtx)
    val newOpenLevel = classOpenLevel(newClass)(using newCtx)

    val isCompatible = (oldOpenLevel, newOpenLevel) match
      case (OpenLevel.Final, _)                                    => true
      case (OpenLevel.Sealed, _)                                   => true
      case (OpenLevel.Default, OpenLevel.Default | OpenLevel.Open) => true
      case (OpenLevel.Open, OpenLevel.Open)                        => true
      case _                                                       => false

    if !isCompatible then
      reportProblem(Problem.RestrictedOpenLevelChange(classInfo(oldClass)(using oldCtx), oldOpenLevel, newOpenLevel))
  end checkOpenLevel

  private def analyzeTypeMember(
    oldPrefix: Type,
    oldSym: TypeMemberSymbol,
    oldIsOverridable: Boolean,
    newPrefix: Type,
    newSym: TypeMemberSymbol
  ): Unit =
    checkVisibility(oldSym, newSym)

    val oldKind = symKind(oldSym)(using oldCtx)
    val newKind = symKind(newSym)(using newCtx)
    val kindsOK = oldKind == newKind // Maybe an abstract type can become a type alias?

    if !kindsOK then reportIncompatibleKindChange(oldSym, oldKind, newKind)
    else () // TODO
  end analyzeTypeMember

  private def analyzeTermMember(
    oldPrefix: ThisType,
    oldSym: TermSymbol,
    oldIsOverridable: Boolean,
    newPrefix: ThisType,
    newSym: TermSymbol
  ): Unit =
    checkVisibility(oldSym, newSym)

    val oldKind = symKind(oldSym)(using oldCtx)
    val newKind = symKind(newSym)(using newCtx)
    val kindsOK =
      import SymbolKind.*
      (oldKind, newKind) match
        case _ if oldKind == newKind => true
        // `def`s and `var`s can become anything (`var` setters are separate)
        case (Method | VarField, _) => true
        // A val can only become a module (it needs to stay stable)
        case (ValField, Module) => true
        // A lazy val can become a val or a module (it needs to stay idempotent)
        case (LazyValField, ValField | Module) => true
        // A var can become a method (setters are handled separately)
        case _ => false
    end kindsOK

    if !kindsOK then reportIncompatibleKindChange(oldSym, oldKind, newKind)
    else
      val oldType = withOldCtx(oldSym.declaredType.widenExpr)
      val translatedOldType = translateType(oldType)
      val newType = withNewCtx(newSym.declaredType.widenExpr.asSeenFrom(newPrefix, newSym.owner))

      val isCompatible = withNewCtx {
        isCompatibleTypeChange(translatedOldType, newType, allowSubtype = !oldIsOverridable)
      }

      if !isCompatible then reportProblem(Problem.IncompatibleTypeChange(symInfo(oldSym)(using oldCtx)))
  end analyzeTermMember

  private def translateType(oldType: Type): Type =
    new TypeTranslator(oldCtx, newCtx).translateType(oldType)

  private def withOldCtx[A](f: Context ?=> A): A = f(using oldCtx)

  private def withNewCtx[A](f: Context ?=> A): A = f(using newCtx)

  private def reportIncompatibleKindChange(oldSymbol: TermOrTypeSymbol, newSymbol: TermOrTypeSymbol): Unit =
    reportIncompatibleKindChange(oldSymbol, symKind(oldSymbol)(using oldCtx), symKind(newSymbol)(using newCtx))

  private def reportIncompatibleKindChange(
    oldSymbol: TermOrTypeSymbol,
    oldKind: SymbolKind,
    newKind: SymbolKind
  ): Unit =
    reportProblem(Problem.IncompatibleKindChange(symInfo(oldSymbol)(using oldCtx), oldKind, newKind))

  private def checkVisibility(oldSymbol: TermOrTypeSymbol, newSymbol: TermOrTypeSymbol): Unit =
    val oldVisibility = symVisibility(oldSymbol)(using oldCtx)
    val newVisibility = symVisibility(newSymbol)(using newCtx)

    if !isValidVisibilityChange(oldVisibility, newVisibility) then
      reportProblem(Problem.RestrictedVisibilityChange(SymbolInfo(pathOf(newSymbol)), oldVisibility, newVisibility))
  end checkVisibility

  private def isValidVisibilityChange(oldVisibility: Visibility, newVisibility: Visibility): Boolean =
    import Visibility.*

    @tailrec
    def isValidPathChange(oldPath: List[Name], newPath: List[Name]): Boolean = (oldPath, newPath) match
      case (oldPathHead :: oldPathTail, newPathHead :: newPathTail) =>
        oldPathHead == newPathHead && isValidPathChange(oldPathTail, newPathTail)
      case (_, _ :: _) =>
        false
      case (_, Nil) =>
        true
    end isValidPathChange

    (oldVisibility, newVisibility) match
      case _ if oldVisibility == newVisibility => true

      case (_, Public)  => true
      case (Private, _) => true // for completeness, but dead code in practice

      case (PackagePrivate(oldPath), PackagePrivate(newPath)) =>
        isValidPathChange(oldPath.path, newPath.path)
      case (PackagePrivate(oldPath), PackageProtected(newPath)) =>
        isValidPathChange(oldPath.path, newPath.path)

      case (Protected, PackageProtected(_)) => true

      case (PackageProtected(oldPath), PackageProtected(newPath)) =>
        isValidPathChange(oldPath.path, newPath.path)

      case _ => false
  end isValidVisibilityChange

  private val openBoundaryMemoized = mutable.AnyRefMap.empty[ClassSymbol, Set[ClassSymbol]]

  /** Returns the "open boundary" of a class.
    *
    * A class `B` is in the open boundary of `A` (`cls`) iff all of the following apply:
    *
    * - `B` is open (or with default openness), and
    * - `B` is a subclass of `A` (incl. reflexivity and transitivy), and
    * - `B == A`, or all the classes from `B` excluded to `A` included are `sealed`.
    *
    * When analyzing a member of `cls`, it can be considered effectively final
    * if it is final in all the classes that belong to the open boundary of
    * `cls`.
    *
    * If the open boundary of a class is empty, all its protected members can
    * be considered private.
    */
  private def classOpenBoundary(cls: ClassSymbol)(using Context): Set[ClassSymbol] =
    def compute: Set[ClassSymbol] =
      if cls.is(Final) then Set.empty
      else if cls.is(Sealed) then
        cls.sealedChildren.toSet.flatMap {
          case childClass: ClassSymbol => classOpenBoundary(childClass)
          case childTerm: TermSymbol   => Set.empty
        }
      else Set(cls)

    openBoundaryMemoized.getOrElseUpdate(cls, compute)
  end classOpenBoundary
end Analyzer

private[tastymima] object Analyzer:
  def classTypeRef(cls: ClassSymbol)(using Context): TypeRef =
    cls.owner match
      case owner: PackageSymbol => TypeRef(owner.packageRef, cls)
      case owner: ClassSymbol   => TypeRef(classThisType(owner), cls)
      case owner                => throw AssertionError(s"unexpected owner $owner of $cls")

  /** The `ThisType` for the given class, as visible from inside this class. */
  def classThisType(cls: ClassSymbol)(using Context): ThisType =
    ThisType(classTypeRef(cls))

  def isAccessible(oldSymbol: Symbol)(using Context): Boolean =
    !oldSymbol.is(Private)

  def symInfo(symbol: Symbol)(using Context): SymbolInfo =
    SymbolInfo(pathOf(symbol))

  def classInfo(symbol: Symbol)(using Context): ClassInfo =
    ClassInfo(pathOf(symbol))

  /** Tests whether the given member is overridable from outside the library. */
  private def memberIsOverridable(symbol: TermOrTypeSymbol, ownerClassOpenBoundary: Set[ClassSymbol])(
    using Context
  ): Boolean =
    if symbol.isClass || symbol.isAnyOf(Final | Private) || symbol.name == nme.Constructor then
      // Fast path
      false
    else
      ownerClassOpenBoundary.exists { openSubclass =>
        val overridingSym = openSubclass.linearization.iterator.map(symbol.overridingSymbol(_)).collectFirst {
          case Some(overridingSym) => overridingSym
        }
        // In any case, we must find `symbol` itself, if it is never overridden
        assert(overridingSym.isDefined, s"Did not find $symbol in open subclass $openSubclass")
        !overridingSym.get.is(Final)
      }
  end memberIsOverridable

  def symVisibility(symbol: TermOrTypeSymbol)(using Context): Visibility =
    if symbol.is(Private) then Visibility.Private
    else if symbol.is(Protected) then
      symbol.privateWithin match
        case None =>
          Visibility.Protected
        case Some(within) =>
          if within.isPackage then Visibility.PackageProtected(symInfo(within))
          else Visibility.Protected
    else
      symbol.privateWithin match
        case None =>
          Visibility.Public
        case Some(within) =>
          if within.isPackage then Visibility.PackagePrivate(symInfo(within))
          else Visibility.Private
  end symVisibility

  def symKind(symbol: TermOrTypeSymbol)(using Context): SymbolKind = symbol match
    case sym: TermSymbol =>
      if sym.is(Module) then SymbolKind.Module
      else if sym.is(Method) then SymbolKind.Method
      else if sym.is(Mutable) then SymbolKind.VarField
      else if sym.is(Lazy) then SymbolKind.LazyValField
      else SymbolKind.ValField
    case _: ClassSymbol =>
      SymbolKind.Class
    case sym: TypeMemberSymbol =>
      sym.typeDef match
        case TypeMemberDefinition.TypeAlias(_)          => SymbolKind.TypeAlias
        case TypeMemberDefinition.AbstractType(_)       => SymbolKind.AbstractTypeMember
        case TypeMemberDefinition.OpaqueTypeAlias(_, _) => SymbolKind.OpaqueTypeAlias
    case _: TypeParamSymbol =>
      SymbolKind.TypeParam
  end symKind

  private def classOpenLevel(cls: ClassSymbol)(using Context): OpenLevel =
    if cls.is(Final) then OpenLevel.Final
    else if cls.is(Sealed) then OpenLevel.Sealed
    else if cls.is(Open) then OpenLevel.Open
    else OpenLevel.Default
  end classOpenLevel

  def pathOf(symbol: Symbol): List[Name] =
    if symbol.isRoot then Nil
    else
      val owner = symbol.owner match
        case owner: DeclaringSymbol => owner
        case owner                  => throw AssertionError(s"Unexpected owner $owner in pathOf($symbol)")
      pathOf(owner) :+ symbol.name
  end pathOf

  /** Check that `oldType.matches(newType)` and that the result types are compatible.
    *
    * If `allowSubtype` is true, the result of `newType` can be any subtype of the result of `oldType`.
    * Otherwise, they must be equivalent.
    */
  private def isCompatibleTypeChange(oldType: Type, newType: Type, allowSubtype: Boolean)(using Context): Boolean =
    oldType.matches(newType) && isFinalResultTypeCompatible(oldType, newType, allowSubtype)

  private def isFinalResultTypeCompatible(oldType: Type, newType: Type, allowSubtype: Boolean)(using Context): Boolean =
    (oldType.widen, newType.widen) match
      case (oldType: MethodType, newType: MethodType) =>
        isFinalResultTypeCompatible(oldType.resultType, newType.instantiate(oldType.paramRefs), allowSubtype)
      case (oldType: PolyType, newType: PolyType) =>
        isFinalResultTypeCompatible(oldType.resultType, newType.instantiate(oldType.paramRefs), allowSubtype)
      case _ =>
        if allowSubtype then newType.isSubtype(oldType)
        else newType.isSameType(oldType)
  end isFinalResultTypeCompatible
end Analyzer
