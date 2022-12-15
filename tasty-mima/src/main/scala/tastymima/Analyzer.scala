package tastymima

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Flags.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Types.*

import Problems.*
import Utils.*

private[tastymima] final class Analyzer(val oldCtx: Context, val newCtx: Context):
  import Analyzer.*

  private val _problems = mutable.ListBuffer.empty[Problem]

  private val translator = new Translator(oldCtx, newCtx)

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
    if !isAccessible(oldTopClass)(using oldCtx) then () // OK
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

    val oldThisType = classThisType(oldClass)(using oldCtx)
    val newThisType = classThisType(newClass)(using newCtx)

    for oldDecl <- oldClass.declarations(using oldCtx) do
      if !isAccessible(oldDecl)(using oldCtx) then () // OK
      else
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
                analyzeTypeMember(oldThisType, oldDecl, newThisType, newDecl)
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
                analyzeTermMember(oldThisType, oldDecl, newThisType, newDecl.asInstanceOf[TermSymbol])
  end analyzeClass

  private def analyzeTypeMember(
    oldPrefix: Type,
    oldSym: TypeMemberSymbol,
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

  private def analyzeTermMember(oldPrefix: Type, oldSym: TermSymbol, newPrefix: Type, newSym: TermSymbol): Unit =
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
    else () // TODO
  end analyzeTermMember

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

  private def checkVisibility(oldSymbol: Symbol, newSymbol: Symbol): Unit =
    if !isAccessible(newSymbol)(using newCtx) then
      reportProblem(Problem.SymbolNotAccessible(SymbolInfo(pathOf(newSymbol))))
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

  def pathOf(symbol: Symbol): List[Name] =
    if symbol.isRoot then Nil
    else
      val owner = symbol.owner match
        case owner: DeclaringSymbol => owner
        case owner                  => throw AssertionError(s"Unexpected owner $owner in pathOf($symbol)")
      pathOf(owner) :+ symbol.name
  end pathOf
end Analyzer
