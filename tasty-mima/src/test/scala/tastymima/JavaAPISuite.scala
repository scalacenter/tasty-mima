package tastymima

import java.net.URL
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

  def createTastyMiMaViaReflection(): intf.TastyMiMa =
    val urls = TestClasspaths.tastyMiMaClasspath.toArray[URL | Null]
    intf.TastyMiMa.newInstance(urls, getClass().getClassLoader()).nn

  test("direct") {
    val tastyMiMa: intf.TastyMiMa = new TastyMiMa()
    val problems = analyzeTestLib(tastyMiMa)

    val oneExpectedProblem =
      intf.ProblemMatcher.make(ProblemKind.MissingClass, "testlib.missingclasses.ClassOnlyInV1").nn
    assert(problems.exists(oneExpectedProblem(_)))
  }

  test("via-reflection") {
    val tastyMiMa: intf.TastyMiMa = createTastyMiMaViaReflection()
    val problems = analyzeTestLib(tastyMiMa)

    val oneExpectedProblem =
      intf.ProblemMatcher.make(ProblemKind.MissingClass, "testlib.missingclasses.ClassOnlyInV1").nn
    assert(problems.exists(oneExpectedProblem(_)))
  }
end JavaAPISuite
