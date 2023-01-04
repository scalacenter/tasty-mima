package tastymima

import java.util.List as JList

import tastymima.intf.ProblemKind

class JavaAPISuite extends munit.FunSuite:
  def analyzeTestLib(tastyMiMa: intf.TastyMiMa): List[intf.Problem] =
    import scala.collection.JavaConverters.*
    import TestClasspaths.*

    val javaProblems =
      tastyMiMa.analyze(testLibV1Paths.asJava, testLibV1EntryPath, testLibV2Paths.asJava, testLibV2EntryPath).nn
    javaProblems.asScala.toList
  end analyzeTestLib

  test("direct") {
    val tastyMiMa: intf.TastyMiMa = new TastyMiMa()
    val problems = analyzeTestLib(tastyMiMa)

    val oneExpectedProblem =
      intf.ProblemMatcher.make(ProblemKind.MissingClass, "testlib.missingclasses.ClassOnlyInV1").nn
    assert(problems.exists(oneExpectedProblem(_)))
  }
end JavaAPISuite
