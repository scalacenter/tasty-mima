package tastymima

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*

class ElementaryLoadingSuite extends munit.FunSuite:
  test("ClassOnlyInV1") {
    val ctx = tastyquery.Contexts.init(TestClasspaths.testLibV1Classpath)
    given Context = ctx

    val ClassOnlyInV1Class = ctx.findSymbolFromRoot(termName("testlib") :: typeName("ClassOnlyInV1") :: Nil).asClass

    val testLibSymbols = ctx.findSymbolsByClasspathEntry(TestClasspaths.testLibV1Entry)
    assert(testLibSymbols.exists(_ == ClassOnlyInV1Class))

    intercept[MemberNotFoundException](ctx.findSymbolFromRoot(termName("testlib") :: typeName("ClassOnlyInV2") :: Nil))
  }

  test("ClassOnlyInV2") {
    val ctx = tastyquery.Contexts.init(TestClasspaths.testLibV2Classpath)
    given Context = ctx

    val ClassOnlyInV2Class = ctx.findSymbolFromRoot(termName("testlib") :: typeName("ClassOnlyInV2") :: Nil).asClass

    val testLibSymbols = ctx.findSymbolsByClasspathEntry(TestClasspaths.testLibV2Entry)
    assert(testLibSymbols.exists(_ == ClassOnlyInV2Class))

    intercept[MemberNotFoundException](ctx.findSymbolFromRoot(termName("testlib") :: typeName("ClassOnlyInV1") :: Nil))
  }
end ElementaryLoadingSuite
