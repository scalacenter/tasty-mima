package tastymima

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*

import tastymima.Problems.*

class AnalyzeSuite extends munit.FunSuite:
  import AnalyzeSuite.*

  def problemsInPackage(packageName: String): List[Problem] =
    val fullPackageName = "testlib." + packageName.name
    val classpaths = TestClasspaths.makeFilteredClasspaths(fullPackageName)
    TastyMiMa.analyze(classpaths._1, classpaths._2, classpaths._3, classpaths._4)

  def assertProblems(actualProblems: List[Problem])(expectedProblemMatchers: ProblemMatcher*): Unit =
    val remainingMatchers = mutable.ListBuffer.from(expectedProblemMatchers)
    val unexpectedProblems = mutable.ListBuffer.empty[Problem]
    for actualProblem <- actualProblems do
      val matcher = remainingMatchers.find(m => m(actualProblem))
      matcher match
        case Some(m) =>
          remainingMatchers -= m
        case None =>
          unexpectedProblems += actualProblem

    if remainingMatchers.nonEmpty || unexpectedProblems.nonEmpty then
      var msg = "Assertion on problem list failed."
      if remainingMatchers.nonEmpty then msg += remainingMatchers.mkString("\nMissing problems:\n* ", "\n* ", "")
      if unexpectedProblems.nonEmpty then msg += unexpectedProblems.mkString("\nUnexpected problems:\n* ", "\n* ", "")
      fail(msg)
  end assertProblems

  test("missing classes") {
    val problems = problemsInPackage("missingclasses")

    assertProblems(problems)(
      ProblemMatcher.MissingClass("testlib.missingclasses.ClassOnlyInV1"),
      ProblemMatcher.MissingClass("testlib.missingclasses.ObjectContainer.ClassOnlyInV1")
    )
  }
end AnalyzeSuite

object AnalyzeSuite:
  trait ProblemMatcher:
    def apply(problem: Problem): Boolean

  object ProblemMatcher:
    final case class MissingClass(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.MissingClass(info) => info.toString() == fullName
        case _                          => false
    end MissingClass
  end ProblemMatcher
end AnalyzeSuite
