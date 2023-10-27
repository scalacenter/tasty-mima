package tastymima

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*

class ElementaryLoadingSuite extends munit.FunSuite:
  test("ClassOnlyInV1") {
    val ctx = Context.initialize(TestClasspaths.testLibV1Classpath)

    val ClassOnlyInV1Class = ctx.findTopLevelClass("testlib.ClassOnlyInV1")

    val testLibSymbols = ctx.findSymbolsByClasspathEntry(TestClasspaths.testLibV1Entry)
    assert(testLibSymbols.exists(_ == ClassOnlyInV1Class))

    intercept[MemberNotFoundException](ctx.findTopLevelClass("testlib.ClassOnlyInV2"))
  }

  test("ClassOnlyInV2") {
    val ctx = Context.initialize(TestClasspaths.testLibV2Classpath)

    val ClassOnlyInV2Class = ctx.findTopLevelClass("testlib.ClassOnlyInV2")

    val testLibSymbols = ctx.findSymbolsByClasspathEntry(TestClasspaths.testLibV2Entry)
    assert(testLibSymbols.exists(_ == ClassOnlyInV2Class))

    intercept[MemberNotFoundException](ctx.findTopLevelClass("testlib.ClassOnlyInV1"))
  }
end ElementaryLoadingSuite
