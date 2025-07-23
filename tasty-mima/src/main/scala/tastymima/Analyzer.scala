package tastymima

import scala.annotation.tailrec

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Modifiers.{OpenLevel, TermSymbolKind, Visibility as TQVisibility}
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Types.*

import tastymima.intf.{Config, ProblemKind, ProblemMatcher}

import Utils.*

private[tastymima] final class Analyzer(val config: Config, val oldCtx: Context, val newCtx: Context):
  import Analyzer.*

  private val problemFilters: List[ProblemMatcher] =
    import scala.jdk.CollectionConverters.*
    config.getProblemFilters().nn.asScala.toList

  private val artifactPrivatePackagePaths: List[List[SimpleName]] =
    import scala.jdk.CollectionConverters.*
    for stringPath <- config.getArtifactPrivatePackages().nn.asScala.toList
    yield stringPath.split('.').toList.map(termName(_))

  private val _problems = mutable.ListBuffer.empty[Problem]

  def analyzeTopSymbols(oldTopSymbols: List[Symbol], newTopSymbols: List[Symbol]): Unit =
    for topSymbol <- oldTopSymbols do
      protect(topSymbol) {
        topSymbol match
          case topClass: ClassSymbol => analyzeTopClass(topClass)
          case _                     => ()
      }
  end analyzeTopSymbols

  def allProblems: List[Problem] =
    _problems.toList

  private def reportProblem(problem: Problem): Unit =
    if !problemFilters.exists(_(problem)) then _problems += problem

  private def reportProblem(kind: ProblemKind, path: List[Name]): Unit =
    reportProblem(Problem(kind, path))

  private def reportProblem(kind: ProblemKind, symbol: Symbol): Unit =
    reportProblem(kind, pathOf(symbol))

  private def reportProblem(kind: ProblemKind, symbol: Symbol, details: Matchable): Unit =
    reportProblem(Problem(kind, pathOf(symbol), details))

  private def reportProblem(kind: ProblemKind, symbol: Symbol, before: Matchable, after: Matchable): Unit =
    reportProblem(kind, symbol, Problem.BeforeAfter(before, after))

  private def analyzeTopClass(oldTopClass: ClassSymbol): Unit =
    val oldVisibility = symVisibility(oldTopClass)
    val isTopAccessible = oldVisibility != Visibility.Private && oldVisibility != Visibility.Protected

    if !isTopAccessible then () // OK
    else
      val path = oldTopClass.owner.asPackage.fullName.path :+ oldTopClass.name
      invalidStructureToOption(newCtx.findSymbolFromRoot(path).asClass) match
        case None =>
          reportProblem(ProblemKind.MissingClass, oldTopClass)
        case Some(newTopClass) =>
          analyzeClass(oldTopClass, newTopClass)
  end analyzeTopClass

  private def analyzeClass(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    checkVisibility(oldClass, newClass)

    val oldKind = symKind(oldClass)
    val newKind = symKind(newClass)
    if oldKind != newKind then
      reportProblem(ProblemKind.IncompatibleKindChange, oldClass, oldKind, newKind)
      return // things can severely break further down, in that case

    val oldTypeParams = oldClass.typeParams
    val newTypeParams = newClass.typeParams
    if oldTypeParams.sizeCompare(newTypeParams) != 0 then
      reportProblem(ProblemKind.TypeArgumentCountMismatch, oldClass, oldTypeParams.size, newTypeParams.size)
      return // things can severely break further down, in that case
    for (oldTypeParam, newTypeParam) <- oldTypeParams.zip(newTypeParams) do
      analyzeClassTypeParam(oldTypeParam, newTypeParam)

    val openBoundary = classOpenBoundary(oldClass)(using oldCtx)

    protect(oldClass) {
      checkClassParents(oldClass, newClass)

      if openBoundary.nonEmpty then checkSelfType(oldClass, newClass)

      checkOpenLevel(oldClass, newClass)

      if oldKind == SymbolKind.Class && !oldClass.isAbstractClass && newClass.isAbstractClass then
        reportProblem(ProblemKind.AbstractClass, oldClass)
    }

    for oldDecl <- oldClass.declarations(using oldCtx) do
      protect(oldDecl) {
        analyzeMemberOfClass(openBoundary, oldDecl, newClass)
      }
    end for // oldDecl

    if openBoundary.nonEmpty && newClass.isAbstractClass then checkNewAbstractMembers(oldClass, openBoundary, newClass)
  end analyzeClass

  private def checkClassParents(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    val oldParents = oldClass.parents(using oldCtx)
    val newParents = newClass.parents(using newCtx)

    for oldParent <- oldParents do
      val translatedOldParent = translateType(oldParent)
      if !newParents.exists(_.isSubType(translatedOldParent)(using newCtx)) then
        reportProblem(ProblemKind.MissingParent, oldClass, oldParent)
  end checkClassParents

  private def checkSelfType(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    val oldSelfType = oldClass.givenSelfType
    val translatedOldSelfType = oldSelfType.map(translateType(_))
    val newSelfType = newClass.givenSelfType

    val isCompatible = (translatedOldSelfType, newSelfType) match
      case (None, None)                             => true
      case (Some(translatedOldType), Some(newType)) => translatedOldType.isSameType(newType)(using newCtx)
      case _                                        => false

    if !isCompatible then reportProblem(ProblemKind.IncompatibleSelfTypeChange, oldClass, oldSelfType, newSelfType)
  end checkSelfType

  private def checkOpenLevel(oldClass: ClassSymbol, newClass: ClassSymbol): Unit =
    val oldOpenLevel = oldClass.openLevel
    val newOpenLevel = newClass.openLevel

    val isCompatible = (oldOpenLevel, newOpenLevel) match
      case (OpenLevel.Final, _)                                  => true
      case (OpenLevel.Sealed, _)                                 => true
      case (OpenLevel.Closed, OpenLevel.Closed | OpenLevel.Open) => true
      case (OpenLevel.Open, OpenLevel.Open)                      => true
      case _                                                     => false

    if !isCompatible then reportProblem(ProblemKind.RestrictedOpenLevelChange, oldClass, oldOpenLevel, newOpenLevel)
  end checkOpenLevel

  private def analyzeClassTypeParam(oldSym: ClassTypeParamSymbol, newSym: ClassTypeParamSymbol): Unit =
    if oldSym.name != newSym.name then
      reportProblem(ProblemKind.IncompatibleNameChange, oldSym, oldSym.name, newSym.name)

    val oldBounds = oldSym.declaredBounds
    val translatedOldBounds = translateTypeBounds(oldBounds)
    val newBounds = newSym.declaredBounds

    if !isCompatibleTypeBoundsChange(translatedOldBounds, newBounds, allowNarrower = false)(using newCtx) then
      reportProblem(ProblemKind.IncompatibleTypeChange, oldSym, oldBounds, newBounds)
  end analyzeClassTypeParam

  private def analyzeMemberOfClass(
    openBoundary: Set[ClassSymbol],
    oldDecl: TermOrTypeSymbol,
    newClass: ClassSymbol
  ): Unit =
    val oldVisibility = symVisibility(oldDecl)
    val isMemberAccessible = oldVisibility match
      case Visibility.Private   => false
      case Visibility.Protected => openBoundary.nonEmpty
      case _                    => true

    if !isMemberAccessible then () // OK
    else
      val newThisType = newClass.thisType

      def oldIsOverridable = memberIsOverridable(oldDecl, openBoundary)(using oldCtx)

      def oldKind = symKind(oldDecl)

      oldDecl match
        case oldDecl: ClassSymbol =>
          newClass.getDecl(oldDecl.name)(using newCtx) match
            case None =>
              reportProblem(ProblemKind.MissingClass, oldDecl)
            case Some(newDecl: ClassSymbol) =>
              analyzeClass(oldDecl, newDecl)
            case Some(newDecl: TypeSymbolWithBounds) =>
              reportProblem(ProblemKind.IncompatibleKindChange, oldDecl, oldKind, symKind(newDecl))

        case oldDecl: TypeMemberSymbol =>
          // Search with getDecl for private members, but fall back on getMember for inherited members
          newClass.getDecl(oldDecl.name)(using newCtx).orElse(newClass.getMember(oldDecl.name)(using newCtx)) match
            case None =>
              reportProblem(ProblemKind.MissingTypeMember, oldDecl)
            case Some(newDecl: TypeMemberSymbol) =>
              analyzeTypeMember(oldDecl, oldIsOverridable, newThisType, newDecl)
            case Some(newDecl) =>
              reportProblem(ProblemKind.IncompatibleKindChange, oldDecl, oldKind, symKind(newDecl))

        case _: TypeParamSymbol =>
          () // nothing to do

        case oldDecl: TermSymbol =>
          lookupCorrespondingTermMember(oldCtx, oldDecl, newCtx, newClass) match
            case None =>
              reportProblem(ProblemKind.MissingTermMember, oldDecl, oldDecl.signature(using oldCtx))
            case Some(newDecl) =>
              analyzeTermMember(oldDecl, oldIsOverridable, newThisType, newDecl)
  end analyzeMemberOfClass

  private def analyzeTypeMember(
    oldSym: TypeMemberSymbol,
    oldIsOverridable: Boolean,
    newPrefix: Type,
    newSym: TypeMemberSymbol
  ): Unit =
    import tastyquery.Symbols.TypeMemberDefinition as TMDef

    checkVisibility(oldSym, newSym)
    checkMemberFinal(oldSym, oldIsOverridable, newSym)

    def reportIncompatibleTypeChange(before: Matchable, after: Matchable): Unit =
      reportProblem(ProblemKind.IncompatibleTypeChange, oldSym, before, after)

    val oldTypeDef = oldSym.typeDef
    val newTypeDef = newSym.typeDef
    val allowNarrower = !oldIsOverridable

    (oldTypeDef, newTypeDef) match
      case (TMDef.TypeAlias(oldAlias), TMDef.TypeAlias(newAlias)) =>
        val translatedOldAlias = translateType(oldAlias)
        if !translatedOldAlias.isSameType(newAlias)(using newCtx) then reportIncompatibleTypeChange(oldAlias, newAlias)

      case (TMDef.AbstractType(oldBounds), TMDef.AbstractType(newBounds)) =>
        val translatedOldBounds = translateTypeBounds(oldBounds)
        if !isCompatibleTypeBoundsChange(translatedOldBounds, newBounds, allowNarrower)(using newCtx) then
          reportIncompatibleTypeChange(oldBounds, newBounds)

      case (TMDef.OpaqueTypeAlias(oldBounds, oldAlias), TMDef.OpaqueTypeAlias(newBounds, newAlias)) =>
        val translatedOldBounds = translateTypeBounds(oldBounds)
        if !isCompatibleTypeBoundsChange(translatedOldBounds, newBounds, allowNarrower)(using newCtx) then
          reportIncompatibleTypeChange(oldBounds, newBounds)

        if oldAlias.isInstanceOf[TypeLambda] || newAlias.isInstanceOf[TypeLambda] then
          /* If either side is a TypeLambda, the types must be equivalent to guarantee
           * that the erasure is always the same, no matter the actual type param.
           */
          val translatedOldAlias = translateType(oldAlias)
          if !translatedOldAlias.isSameType(newAlias)(using newCtx) then
            reportIncompatibleTypeChange(oldAlias, newAlias)
        else
          /* Otherwise, the type can change as long as the erasure remains the same.
           * Since opaque type aliases only exist in Scala 3, we always erase for that language.
           */
          import tastyquery.SourceLanguage.Scala3
          val oldErasedAlias = ErasedTypeRef.erase(oldAlias, Scala3)(using oldCtx)
          val newErasedAlias = ErasedTypeRef.erase(newAlias, Scala3)(using newCtx)
          if !erasedTypesCorrespond(oldErasedAlias, newErasedAlias) then
            reportIncompatibleTypeChange(oldErasedAlias, newErasedAlias)
        end if

      case _ =>
        val oldKind = symKind(oldSym)
        val newKind = symKind(newSym)
        reportProblem(ProblemKind.IncompatibleKindChange, oldSym, oldKind, newKind)
  end analyzeTypeMember

  private def analyzeTermMember(
    oldSym: TermSymbol,
    oldIsOverridable: Boolean,
    newPrefix: ThisType,
    newSym: TermSymbol
  ): Unit =
    checkVisibility(oldSym, newSym)
    checkMemberFinal(oldSym, oldIsOverridable, newSym)

    val oldKind = symKind(oldSym)
    val newKind = symKind(newSym)
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

    if !kindsOK then reportProblem(ProblemKind.IncompatibleKindChange, oldSym, oldKind, newKind)
    else
      val oldType = oldSym.declaredType
      val translatedOldType = translateTypeOrMethodic(oldType)
      val newType = newSym.declaredType.asSeenFrom(newPrefix, newSym.owner)(using newCtx)

      val isCompatible =
        isCompatibleTypeChange(translatedOldType, newType, allowSubtype = !oldIsOverridable)(using newCtx)

      if !isCompatible then reportProblem(ProblemKind.IncompatibleTypeChange, oldSym, oldType, newType)
  end analyzeTermMember

  private def checkMemberFinal(oldSym: TermOrTypeSymbol, oldIsOverridable: Boolean, newSym: TermOrTypeSymbol): Unit =
    if oldIsOverridable && newSym.isFinalMember then reportProblem(ProblemKind.FinalMember, oldSym)
  end checkMemberFinal

  private def checkNewAbstractMembers(
    oldClass: ClassSymbol,
    oldOpenBoundary: Set[ClassSymbol],
    newClass: ClassSymbol
  ): Unit =
    val newOpenBoundary = classOpenBoundary(newClass)(using newCtx)

    /* The open boundary that is in common between the old and new versions.
     * Since open boundaries never include local classes (by construction), we
     * can use `fullNameOfNonLocalSymbol` to reliably identify classes.
     */
    val commonOpenBoundary: Set[(ClassSymbol, ClassSymbol)] =
      for
        oldSubclass <- oldOpenBoundary
        oldFullName = fullNameOfNonLocalSymbol(oldSubclass)
        newSubclass <- newOpenBoundary.find(c => fullNameOfNonLocalSymbol(c) == oldFullName)
      yield (oldSubclass, newSubclass)

    if commonOpenBoundary.isEmpty then
      // Fast path, nothing to do
      // (fast-path because the `commonOpenBoundary.forall` in checkNewMaybeAbstractTermMember would always be true)
      (): Unit // : Unit to prevent scalafmt from killing this line
    else
      for case (newDecl: TermSymbol) <- newClass.declarations(using newCtx) do
        protect(newDecl) {
          checkNewMaybeAbstractTermMember(commonOpenBoundary, newDecl)
        }
  end checkNewAbstractMembers

  private def checkNewMaybeAbstractTermMember(
    commonOpenBoundary: Set[(ClassSymbol, ClassSymbol)],
    newDecl: TermSymbol
  ): Unit =
    // If the member is abstract
    if newDecl.isAbstractMember && newDecl.name != nme.Constructor then
      // If it is actually abstract in at least one subclass of the open boundary, ...
      val newIsActuallyAbstract = commonOpenBoundary.exists { (oldSubclass, newSubclass) =>
        isActuallyAbstractIn(newDecl, newSubclass)(using newCtx)
      }
      if newIsActuallyAbstract then
        // Unless it was already actually abstract in 'old' in *all* the subclasses of the open boundary, ...
        val oldIsAbstractEverywhere = commonOpenBoundary.forall { (oldSubclass, newSubclass) =>
          lookupCorrespondingTermMember(newCtx, newDecl, oldCtx, oldSubclass) match
            case None          => false
            case Some(oldDecl) => isActuallyAbstractIn(oldDecl, oldSubclass)(using oldCtx)
        }
        if !oldIsAbstractEverywhere then
          // Then it is a problem
          reportProblem(ProblemKind.NewAbstractMember, newDecl, newDecl.signature(using newCtx))
  end checkNewMaybeAbstractTermMember

  private def translateType(oldType: Type): Type =
    new TypeTranslator(oldCtx, newCtx).translateType(oldType)

  private def translateTypeOrMethodic(oldType: TypeOrMethodic): TypeOrMethodic =
    new TypeTranslator(oldCtx, newCtx).translateTypeOrMethodic(oldType)

  private def translateTypeBounds(oldBounds: TypeBounds): TypeBounds =
    new TypeTranslator(oldCtx, newCtx).translateTypeBounds(oldBounds)

  private def checkVisibility(oldSymbol: TermOrTypeSymbol, newSymbol: TermOrTypeSymbol): Unit =
    val oldVisibility = symVisibility(oldSymbol)
    val newVisibility = symVisibility(newSymbol)

    if !isValidVisibilityChange(oldVisibility, newVisibility) then
      reportProblem(ProblemKind.RestrictedVisibilityChange, oldSymbol, oldVisibility, newVisibility)
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
        isValidPathChange(oldPath, newPath)
      case (PackagePrivate(oldPath), PackageProtected(newPath)) =>
        isValidPathChange(oldPath, newPath)

      case (Protected, PackageProtected(_)) => true

      case (PackageProtected(oldPath), PackageProtected(newPath)) =>
        isValidPathChange(oldPath, newPath)

      case _ => false
  end isValidVisibilityChange

  private val openBoundaryMemoized = mutable.HashMap.empty[ClassSymbol, Set[ClassSymbol]]

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
    def compute: Set[ClassSymbol] = cls.openLevel match
      case OpenLevel.Final =>
        Set.empty
      case OpenLevel.Sealed =>
        cls.sealedChildren.toSet.flatMap {
          case childClass: ClassSymbol =>
            /* #36 If a sealed class has local children, it appears itself in
             * its `sealedChildren` list. Since local child classes can never
             * be extended from outside, they do not contribute to the open
             * boundary. We must cut it off here to avoid an infinite recursion.
             */
            if childClass == cls then Set.empty
            else classOpenBoundary(childClass)
          case childTerm: TermSymbol =>
            Set.empty
        }
      case OpenLevel.Closed | OpenLevel.Open =>
        Set(cls)
    end compute

    openBoundaryMemoized.getOrElseUpdate(cls, compute)
  end classOpenBoundary

  def symVisibility(symbol: TermOrTypeSymbol): Visibility =
    def isArtifactPrivate(packagePath: List[Name]): Boolean =
      artifactPrivatePackagePaths.exists(path => packagePath.startsWith(path))

    symbol.visibility match
      case TQVisibility.PrivateThis | TQVisibility.Private =>
        Visibility.Private

      case TQVisibility.ScopedPrivate(scope) =>
        if scope.isPackage then
          val packagePath = pathOf(scope)
          if isArtifactPrivate(packagePath) then Visibility.Private
          else Visibility.PackagePrivate(packagePath)
        else Visibility.Private

      case TQVisibility.ProtectedThis | TQVisibility.Protected =>
        Visibility.Protected

      case TQVisibility.ScopedProtected(scope) =>
        if scope.isPackage then
          val packagePath = pathOf(scope)
          if isArtifactPrivate(packagePath) then Visibility.Protected
          else Visibility.PackageProtected(packagePath)
        else Visibility.Protected

      case TQVisibility.Public =>
        Visibility.Public
  end symVisibility

  private def protect(problemSym: Symbol)(op: => Unit): Unit =
    try op
    catch
      case error: Exception =>
        reportProblem(ProblemKind.InternalError, problemSym, error)
  end protect
end Analyzer

private[tastymima] object Analyzer:
  /** Visibility of a symbol, from the API point of view.
    *
    * Note that any qualified-private with a scope narrower than a package is
    * considered full `private` from the API point of view. Likewise, any
    * qualified-protected with a scope narrower than a package is considered
    * full `protected`.
    */
  enum Visibility:
    case Private
    case PackagePrivate(scope: List[Name])
    case Protected
    case PackageProtected(scope: List[Name])
    case Public

    override def toString(): String = this match
      case Private                 => "private"
      case PackagePrivate(scope)   => scope.mkString("package[", ".", "]")
      case Protected               => "protected"
      case PackageProtected(scope) => scope.mkString("protected[", ".", "]")
      case Public                  => "public"
    end toString
  end Visibility

  enum SymbolKind:
    case Class, Trait, TypeAlias, AbstractTypeMember, OpaqueTypeAlias, TypeParam
    case Module, Method, ValField, VarField, LazyValField

    override def toString(): String = this match
      case Class              => "class"
      case Trait              => "trait"
      case TypeAlias          => "type alias"
      case AbstractTypeMember => "abstract type member"
      case OpaqueTypeAlias    => "opaque type alias"
      case TypeParam          => "type parameter"
      case Module             => "object"
      case Method             => "def"
      case ValField           => "val"
      case VarField           => "var"
      case LazyValField       => "lazy val"
    end toString
  end SymbolKind

  private def fullNameOfNonLocalSymbol(sym: Symbol): List[UnsignedName] =
    /* This actually gives a result even for local symbols.
     * In that case it is not guaranteed to be unique.
     */
    def loop(sym: Symbol, acc: List[UnsignedName]): List[UnsignedName] = sym match
      case sym: TermOrTypeSymbol => loop(sym.owner, sym.name :: acc)
      case sym: PackageSymbol    => sym.fullName.path ::: acc.reverse

    loop(sym, Nil)
  end fullNameOfNonLocalSymbol

  /** Tests whether the given member is overridable from outside the library. */
  private def memberIsOverridable(symbol: TermOrTypeSymbol, ownerClassOpenBoundary: Set[ClassSymbol])(
    using Context
  ): Boolean =
    if symbol.isFinalMember || symbol.isPrivate || symbol.name == nme.Constructor then
      // Fast path
      false
    else
      ownerClassOpenBoundary.exists { openSubclass =>
        val overridingSym = openSubclass.linearization.iterator.map(symbol.overridingSymbol(_)).collectFirst {
          case Some(overridingSym) => overridingSym
        }
        // In any case, we must find `symbol` itself, if it is never overridden
        assert(overridingSym.isDefined, s"Did not find $symbol in open subclass $openSubclass")
        !overridingSym.get.isFinalMember
      }
  end memberIsOverridable

  def symKind(symbol: TermOrTypeSymbol): SymbolKind = symbol match
    case sym: TermSymbol =>
      sym.kind match
        case TermSymbolKind.Module  => SymbolKind.Module
        case TermSymbolKind.Val     => SymbolKind.ValField
        case TermSymbolKind.LazyVal => SymbolKind.LazyValField
        case TermSymbolKind.Var     => SymbolKind.VarField
        case TermSymbolKind.Def     => SymbolKind.Method
    case sym: ClassSymbol =>
      if sym.isTrait then SymbolKind.Trait
      else SymbolKind.Class
    case sym: TypeMemberSymbol =>
      sym.typeDef match
        case TypeMemberDefinition.TypeAlias(_)          => SymbolKind.TypeAlias
        case TypeMemberDefinition.AbstractType(_)       => SymbolKind.AbstractTypeMember
        case TypeMemberDefinition.OpaqueTypeAlias(_, _) => SymbolKind.OpaqueTypeAlias
    case _: TypeParamSymbol =>
      SymbolKind.TypeParam
  end symKind

  private def lookupCorrespondingTermMember(
    fromCtx: Context,
    fromDecl: TermSymbol,
    toCtx: Context,
    toClass: ClassSymbol
  ): Option[TermSymbol] =
    // Search with getDecl for private members, but fall back on getMember for inherited members
    val signedName = fromDecl.signedName(using fromCtx)
    toClass.getDecl(signedName)(using toCtx).orElse(toClass.getMember(signedName)(using toCtx))
  end lookupCorrespondingTermMember

  private def isActuallyAbstractIn(sym: TermSymbol, subclass: ClassSymbol)(using Context): Boolean =
    !subclass.linearization.exists { inClass =>
      sym.matchingSymbol(inClass, subclass).exists(!_.isAbstractMember)
    }
  end isActuallyAbstractIn

  def pathOf(symbol: Symbol): List[Name] =
    if symbol.isPackage && symbol.asPackage.isRootPackage then Nil
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
  private def isCompatibleTypeChange(oldType: TypeOrMethodic, newType: TypeOrMethodic, allowSubtype: Boolean)(
    using Context
  ): Boolean =
    oldType.matches(newType) && isFinalResultTypeCompatible(oldType, newType, allowSubtype)

  private def isFinalResultTypeCompatible(oldType: TypeOrMethodic, newType: TypeOrMethodic, allowSubtype: Boolean)(
    using Context
  ): Boolean =
    (oldType, newType) match
      case (oldType: MethodType, newType: MethodType) =>
        isFinalResultTypeCompatible(oldType.resultType, newType.instantiate(oldType.paramRefs), allowSubtype)
      case (oldType: PolyType, newType: PolyType) =>
        isFinalResultTypeCompatible(oldType.resultType, newType.instantiate(oldType.paramRefs), allowSubtype)
      case (oldType: Type, newType: Type) =>
        if allowSubtype then newType.isSubType(oldType)
        else newType.isSameType(oldType)
      case _ =>
        false
  end isFinalResultTypeCompatible

  private def isCompatibleTypeBoundsChange(oldBounds: TypeBounds, newBounds: TypeBounds, allowNarrower: Boolean)(
    using Context
  ): Boolean =
    (oldBounds, newBounds) match
      case (oldBounds: AbstractTypeBounds, newBounds: AbstractTypeBounds) =>
        if allowNarrower then oldBounds.contains(newBounds)
        else oldBounds.isSameBounds(newBounds)
      case (TypeAlias(oldAlias), TypeAlias(newAlias)) =>
        oldAlias.isSameType(newAlias)
      case _ =>
        false
  end isCompatibleTypeBoundsChange

  @tailrec
  private def erasedTypesCorrespond(tp1: ErasedTypeRef, tp2: ErasedTypeRef): Boolean = (tp1, tp2) match
    case (ErasedTypeRef.ClassRef(cls1), ErasedTypeRef.ClassRef(cls2)) =>
      pathOf(cls1) == pathOf(cls2)
    case (ErasedTypeRef.ArrayTypeRef(base1, dims1), ErasedTypeRef.ArrayTypeRef(base2, dims2)) =>
      dims1 == dims2 && erasedTypesCorrespond(base1, base2)
    case _ =>
      false
  end erasedTypesCorrespond
end Analyzer
