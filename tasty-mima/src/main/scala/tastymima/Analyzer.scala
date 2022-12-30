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

    val oldKind = symKind(oldClass)(using oldCtx)
    val newKind = symKind(newClass)(using newCtx)
    if oldKind != newKind then
      reportIncompatibleKindChange(oldClass, oldKind, newKind)
      return // things can severely break further down, in that case

    val oldTypeParams = oldClass.typeParams(using oldCtx)
    val newTypeParams = newClass.typeParams(using newCtx)
    if oldTypeParams.sizeCompare(newTypeParams) != 0 then
      reportProblem(Problem.TypeArgumentCountMismatch(classInfo(oldClass)(using oldCtx)))
      return // things can severely break further down, in that case
    for (oldTypeParam, newTypeParam) <- oldTypeParams.zip(newTypeParams) do
      analyzeClassTypeParam(oldTypeParam, newTypeParam)

    val openBoundary = classOpenBoundary(oldClass)(using oldCtx)

    checkClassParents(oldClass, newClass)

    if openBoundary.nonEmpty then checkSelfType(oldClass, newClass)

    checkOpenLevel(oldClass, newClass)

    val oldThisType = classThisType(oldClass)(using oldCtx)
    val newThisType = classThisType(newClass)(using newCtx)

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
            lookupCorrespondingTermMember(oldCtx, oldDecl, newCtx, newThisType) match
              case None =>
                reportProblem(Problem.MissingTermMember(symInfo(oldDecl)(using oldCtx)))
              case Some(newDecl) =>
                analyzeTermMember(oldThisType, oldDecl, oldIsOverridable, newThisType, newDecl)
    end for // oldDecl

    if openBoundary.nonEmpty && newClass.isAnyOf(Abstract | Trait) then
      checkNewAbstractMembers(oldClass, openBoundary, newClass)
  end analyzeClass

  private def checkClassParents(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    val oldParents = oldClass.parents(using oldCtx)
    val newParents = newClass.parents(using newCtx)

    for oldParent <- oldParents do
      val translatedOldParent = translateType(oldParent)
      if !newParents.exists(_.isSubtype(translatedOldParent)(using newCtx)) then
        reportProblem(Problem.MissingParent(classInfo(oldClass)(using oldCtx)))
  end checkClassParents

  private def checkSelfType(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    val oldSelfType = oldClass.givenSelfType(using oldCtx)
    val translatedOldSelfType = oldSelfType.map(translateType(_))
    val newSelfType = newClass.givenSelfType(using newCtx)

    val isCompatible = (translatedOldSelfType, newSelfType) match
      case (None, None)                             => true
      case (Some(translatedOldType), Some(newType)) => translatedOldType.isSameType(newType)(using newCtx)
      case _                                        => false

    if !isCompatible then reportProblem(Problem.IncompatibleSelfTypeChange(classInfo(oldClass)(using oldCtx)))
  end checkSelfType

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

  private def analyzeClassTypeParam(oldSym: ClassTypeParamSymbol, newSym: ClassTypeParamSymbol): Unit =
    val oldBounds = oldSym.bounds(using oldCtx)
    val translatedOldBounds = translateTypeBounds(oldBounds)
    val newBounds = newSym.bounds(using newCtx)

    if !isCompatibleTypeBoundsChange(translatedOldBounds, newBounds, allowNarrower = false)(using newCtx) then
      reportProblem(Problem.IncompatibleTypeChange(symInfo(oldSym)(using oldCtx)))
  end analyzeClassTypeParam

  private def analyzeTypeMember(
    oldPrefix: Type,
    oldSym: TypeMemberSymbol,
    oldIsOverridable: Boolean,
    newPrefix: Type,
    newSym: TypeMemberSymbol
  ): Unit =
    import tastyquery.Symbols.TypeMemberDefinition as TMDef

    checkVisibility(oldSym, newSym)

    def reportIncompatibleTypeChange(): Unit =
      reportProblem(Problem.IncompatibleTypeChange(symInfo(oldSym)(using oldCtx)))

    val oldTypeDef = oldSym.typeDef(using oldCtx)
    val newTypeDef = newSym.typeDef(using newCtx)
    val allowNarrower = !oldIsOverridable

    (oldTypeDef, newTypeDef) match
      case (TMDef.TypeAlias(oldAlias), TMDef.TypeAlias(newAlias)) =>
        val translatedOldAlias = translateType(oldAlias)
        if !translatedOldAlias.isSameType(newAlias)(using newCtx) then reportIncompatibleTypeChange()

      case (TMDef.AbstractType(oldBounds), TMDef.AbstractType(newBounds)) =>
        val translatedOldBounds = translateTypeBounds(oldBounds)
        if !isCompatibleTypeBoundsChange(translatedOldBounds, newBounds, allowNarrower)(using newCtx) then
          reportIncompatibleTypeChange()

      case (TMDef.OpaqueTypeAlias(oldBounds, oldAlias), TMDef.OpaqueTypeAlias(newBounds, newAlias)) =>
        val translatedOldBounds = translateTypeBounds(oldBounds)
        if !isCompatibleTypeBoundsChange(translatedOldBounds, newBounds, allowNarrower)(using newCtx) then
          reportIncompatibleTypeChange()

        val oldErasedAlias = ErasedTypeRef.erase(oldAlias)(using oldCtx)
        val newErasedAlias = ErasedTypeRef.erase(newAlias)(using newCtx)
        if oldErasedAlias.toSigFullName != newErasedAlias.toSigFullName then reportIncompatibleTypeChange()

      case _ =>
        val oldKind = symKind(oldSym)(using oldCtx)
        val newKind = symKind(newSym)(using newCtx)
        reportIncompatibleKindChange(oldSym, oldKind, newKind)
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

  private def checkNewAbstractMembers(
    oldClass: ClassSymbol,
    oldOpenBoundary: Set[ClassSymbol],
    newClass: ClassSymbol
  ): Unit =
    val newOpenBoundary = classOpenBoundary(newClass)(using newCtx)

    // The open boundary that is in common between the old and new versions
    val commonOpenBoundary: Set[(ClassSymbol, ClassSymbol)] =
      for
        oldSubclass <- oldOpenBoundary
        oldFullName = oldSubclass.fullName
        newSubclass <- newOpenBoundary.find(_.fullName == oldFullName)
      yield (oldSubclass, newSubclass)

    if commonOpenBoundary.isEmpty then
      // Fast path, nothing to do (fast-path because the `commonOpenBoundary.forall` would always be true)
      (): Unit // : Unit to prevent scalafmt from killing this line
    else
      // For each abstract term member in the new class, ...
      for
        case (newDecl: TermSymbol) <- newClass.declarations(using newCtx)
        if newDecl.is(Abstract) && newDecl.name != nme.Constructor
      do
        // If it is actually abstract in at least one subclass of the open boundary, ...
        val newIsActuallyAbstract = commonOpenBoundary.exists { (oldSubclass, newSubclass) =>
          isActuallyAbstractIn(newDecl, newSubclass)(using newCtx)
        }
        if newIsActuallyAbstract then
          // Unless it was already actually abstract in 'old' in *all* the subclasses of the open boundary, ...
          val oldIsAbstractEverywhere = commonOpenBoundary.forall { (oldSubclass, newSubclass) =>
            lookupCorrespondingTermMember(newCtx, newDecl, oldCtx, classThisType(oldSubclass)(using oldCtx)) match
              case None          => false
              case Some(oldDecl) => isActuallyAbstractIn(oldDecl, oldSubclass)(using oldCtx)
          }
          if !oldIsAbstractEverywhere then
            // Then it is a problem
            reportProblem(Problem.NewAbstractMember(symInfo(newDecl)(using newCtx)))
  end checkNewAbstractMembers

  private def translateType(oldType: Type): Type =
    new TypeTranslator(oldCtx, newCtx).translateType(oldType)

  private def translateTypeBounds(oldBounds: TypeBounds): TypeBounds =
    new TypeTranslator(oldCtx, newCtx).translateTypeBounds(oldBounds)

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
      if symbol.is(Trait) then SymbolKind.Trait
      else SymbolKind.Class
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

  private def lookupCorrespondingTermMember(
    fromCtx: Context,
    fromDecl: TermSymbol,
    toCtx: Context,
    toThisType: ThisType
  ): Option[TermSymbol] =
    val signedName =
      if fromDecl.is(Method) && fromDecl.declaredType(using fromCtx).isInstanceOf[ExprType] then fromDecl.name
      else fromDecl.signedName(using fromCtx)
    memberNotFoundToOption(toThisType.member(signedName)(using toCtx).asTerm)
  end lookupCorrespondingTermMember

  private def isActuallyAbstractIn(sym: TermSymbol, subclass: ClassSymbol)(using Context): Boolean =
    !subclass.linearization.exists { inClass =>
      sym.matchingSymbol(inClass, subclass).exists(!_.is(Abstract))
    }
  end isActuallyAbstractIn

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

  private def isCompatibleTypeBoundsChange(oldBounds: TypeBounds, newBounds: TypeBounds, allowNarrower: Boolean)(
    using Context
  ): Boolean =
    (oldBounds, newBounds) match
      case (RealTypeBounds(oldLow, oldHigh), RealTypeBounds(newLow, newHigh)) =>
        if allowNarrower then oldLow.isSubtype(newLow) && newHigh.isSubtype(oldHigh)
        else oldLow.isSameType(newLow) && newHigh.isSameType(oldHigh)
      case (TypeAlias(oldAlias), TypeAlias(newAlias)) =>
        oldAlias.isSameType(newAlias)
      case _ =>
        false
  end isCompatibleTypeBoundsChange
end Analyzer
