package tastymima

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Flags.*
import tastyquery.Names.*
import tastyquery.Symbols.*

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

    for oldDecl <- oldClass.declarations(using oldCtx) do
      if !isAccessible(oldDecl)(using oldCtx) then () // OK
      else
        oldDecl match
          case oldDecl: ClassSymbol =>
            newClass.getDecl(oldDecl.name)(using newCtx) match
              case None =>
                reportProblem(Problem.MissingClass(ClassInfo(oldDecl.fullName.path)))
              case Some(newDecl: ClassSymbol) =>
                analyzeClass(oldDecl, newDecl)
              case Some(newDecl: TypeSymbolWithBounds) =>
                () // TODO

          case _ =>
            () // TODO
  end analyzeClass

  private def checkVisibility(oldSymbol: Symbol, newSymbol: Symbol): Unit =
    if !isAccessible(newSymbol)(using newCtx) then
      reportProblem(Problem.SymbolNotAccessible(SymbolInfo(pathOf(newSymbol))))
end Analyzer

private[tastymima] object Analyzer:
  def isAccessible(oldSymbol: Symbol)(using Context): Boolean =
    !oldSymbol.is(Private)

  def pathOf(symbol: Symbol): List[Name] =
    if symbol.isRoot then Nil
    else
      val owner = symbol.owner match
        case owner: DeclaringSymbol => owner
        case owner                  => throw AssertionError(s"Unexpected owner $owner in pathOf($symbol)")
      pathOf(owner) :+ symbol.name
  end pathOf
end Analyzer
