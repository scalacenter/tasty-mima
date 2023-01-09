package tastymima.intf

import ProblemKind.*

class ProblemSuite extends munit.FunSuite:
  import ProblemSuite.*

  test("getDescription") {
    def testDesc(kind: ProblemKind, expected: String)(using munit.Location): Unit =
      assert(clue(TestProblem(kind, "<path-string>").getDescription() == clue(expected)))

    testDesc(MissingClass, "The class <path-string> does not have a correspondant in current version")
    testDesc(MissingTypeMember, "The type member <path-string> does not have a correspondant in current version")
    testDesc(MissingTermMember, "The member <path-string> does not have a correspondant in current version")
    testDesc(
      RestrictedVisibilityChange,
      "The symbol <path-string> has a more restrictive visibility qualifier in current version"
    )
    testDesc(IncompatibleKindChange, "The symbol <path-string> has an incompatible kind in current version")
    testDesc(MissingParent, "The class <path-string> is missing a parent in current version")
    testDesc(IncompatibleSelfTypeChange, "The class <path-string> has an incompatible self type in current version")
    testDesc(
      RestrictedOpenLevelChange,
      "The class <path-string> has a more restrictive open level (open, sealed, final) in current version"
    )
    testDesc(AbstractClass, "The class <path-string> was concrete but is abstract in current version")
    testDesc(FinalMember, "The member <path-string> was open but is final in current version")
    testDesc(
      TypeArgumentCountMismatch,
      "The class <path-string> does not have the same number of type arguments in current version"
    )
    testDesc(IncompatibleTypeChange, "The symbol <path-string> has an incompatible type in current version")
    testDesc(
      NewAbstractMember,
      "The member <path-string> was concrete or did not exist but is abstract in current version"
    )
  }

  test("getFilterIncantation") {
    def testIncantation(kind: ProblemKind, expected: String)(using munit.Location): Unit =
      assert(clue(TestProblem(kind, "<path-string>").getFilterIncantation()) == clue(expected))

    testIncantation(MissingClass, """ProblemMatcher.make(ProblemKind.MissingClass, "<path-string>")""")
    testIncantation(FinalMember, """ProblemMatcher.make(ProblemKind.FinalMember, "<path-string>")""")
  }
end ProblemSuite

object ProblemSuite:
  final class TestProblem(kind: ProblemKind, pathString: String) extends Problem:
    def getKind(): ProblemKind = kind
    def getPathString(): String = pathString
  end TestProblem
end ProblemSuite
