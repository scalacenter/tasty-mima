package tastymima.intf

class ProblemMatcherSuite extends munit.FunSuite:
  import ProblemMatcherSuite.*

  test("exact-match") {
    val matcher1 = ProblemMatcher.make(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz$").nn

    assert(matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz$")))

    assert(!matcher1(makeProblem(ProblemKind.IncompatibleKindChange, "foo.bar.Baz$")))

    assert(!matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz")))
    assert(!matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz$.more")))
    assert(!matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo_bar.Baz$")))

    val matcher2 = ProblemMatcher.make("foo.\\*.B\\\\az").nn

    assert(matcher2(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.*.B\\az")))
    assert(matcher2(makeProblem(ProblemKind.IncompatibleKindChange, "foo.*.B\\az")))

    assert(!matcher2(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.B\\az")))
  }

  test("star-match") {
    val matcher1 = ProblemMatcher.make(ProblemKind.IncompatibleTypeChange, "foo.bar.*").nn

    assert(matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz$")))
    assert(matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz")))
    assert(matcher1(makeProblem(ProblemKind.IncompatibleTypeChange, "foo.bar.Baz.Babar")))

    assert(!matcher1(makeProblem(ProblemKind.IncompatibleKindChange, "foo.bar.Baz$")))

    assert(!matcher1(makeProblem(ProblemKind.IncompatibleKindChange, "foo.baz.Bar")))
  }
end ProblemMatcherSuite

object ProblemMatcherSuite:
  def makeProblem(kind: ProblemKind, pathString: String): Problem =
    new Problem {
      def getKind(): ProblemKind = kind
      def getPathString(): String = pathString
    }
  end makeProblem
end ProblemMatcherSuite
