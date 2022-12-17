package tastymima

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*

import tastymima.Problems.*

class AnalyzeSuite extends munit.FunSuite:
  import AnalyzeSuite.*
  import ProblemMatcher as PM

  def problemsInPackage(packageName: String): List[Problem] =
    val fullPackageName = "testlib." + packageName.name
    val classpaths = TestClasspaths.makeFilteredClasspaths(fullPackageName)
    TastyMiMa.analyze(classpaths._1, classpaths._2, classpaths._3, classpaths._4)

  def assertProblems(using munit.Location)(actualProblems: List[Problem])(
    expectedProblemMatchers: ProblemMatcher*
  ): Unit =
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
      PM.MissingClass("testlib.missingclasses.ClassOnlyInV1"),
      PM.MissingClass("testlib.missingclasses.ObjectContainer.ClassOnlyInV1")
    )
  }

  test("missing members") {
    val problems = problemsInPackage("missingmembers")

    assertProblems(problems)(
      // Terms
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedVal"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedVar"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedVar_="),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedDef"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedModule"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedLazyVal"),
      // Module class
      PM.MissingClass("testlib.missingmembers.MissingMembers.removedModule$"),
      // Types
      PM.MissingTypeMember("testlib.missingmembers.MissingMembers.removedTypeAlias"),
      PM.MissingTypeMember("testlib.missingmembers.MissingMembers.removedAbstractType"),
      PM.MissingTypeMember("testlib.missingmembers.MissingMembers.removedOpaqueTypeAlias")
    )
  }

  test("inheritance with member removals") {
    val problems = problemsInPackage("inheritancememberremovals")

    assertProblems(problems)(
      PM.MissingTermMember("testlib.inheritancememberremovals.Child.fieldNotCoveredByParents"),
      PM.MissingTermMember("testlib.inheritancememberremovals.Child.methodNotCoveredByParents")
    )
  }

  test("member kind changes") {
    import SymbolKind.*

    val problems = problemsInPackage("memberkindchanges")

    assertProblems(problems)(
      // val to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.valToVar", ValField, VarField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.valToDef", ValField, Method),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.valToLazyVal", ValField, LazyValField),
      // var to * -> the setters show up
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToVal_="),
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToDef_="),
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToModule_="),
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToLazyVal_="),
      // module to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToVal", Module, ValField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToVar", Module, VarField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToDef", Module, Method),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToLazyVal", Module, LazyValField),
      // lazy val to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.lazyValToVar", LazyValField, VarField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.lazyValToDef", LazyValField, Method),
      // side effects of module to *
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToVal$"),
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToVar$"),
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToDef$"),
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToLazyVal$")
    )
  }

  test("member type changes") {
    val problems = problemsInPackage("membertypechanges")

    assertProblems(problems)(
      // Simple term members
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.valOtherType"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.varOtherType"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.defOtherType"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.lazyValOtherType"),
      // Method members whose change of result type causes a signature change -> they disappear
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.methodSubResultType"),
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.methodOtherResultType"),
      // Method members that keep the same signature despite different result types
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.methodSameSigOtherResultType"),
      // Side effects of changing the type of a var
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.varSubType_="),
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.varOtherType_=")
    )
  }

  test("class type parameters") {
    val problems = problemsInPackage("classtypeparams")

    assertProblems(problems)(
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.a3"),
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.b3"),
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.Inner.c4"),
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.Inner.d4"),
      PM.TypeArgumentCountMismatch("testlib.classtypeparams.ClassTypeParams.ArgCountMismatch")
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

    final case class MissingTypeMember(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.MissingTypeMember(info) => info.toString() == fullName
        case _                               => false
    end MissingTypeMember

    final case class MissingTermMember(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.MissingTermMember(info) => info.toString() == fullName
        case _                               => false
    end MissingTermMember

    final case class IncompatibleKindChange(fullName: String, oldKind: SymbolKind, newKind: SymbolKind)
        extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.IncompatibleKindChange(info, `oldKind`, `newKind`) => info.toString() == fullName
        case _                                                          => false
    end IncompatibleKindChange

    final case class TypeArgumentCountMismatch(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.TypeArgumentCountMismatch(info) => info.toString() == fullName
        case _                                       => false
    end TypeArgumentCountMismatch

    final case class IncompatibleTypeChange(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.IncompatibleTypeChange(info) => info.toString() == fullName
        case _                                    => false
    end IncompatibleTypeChange
  end ProblemMatcher
end AnalyzeSuite
