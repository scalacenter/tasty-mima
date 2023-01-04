package tastymima

import java.nio.file.Path

import tastyquery.Classpaths.*
import tastyquery.Contexts
import tastyquery.jdk.ClasspathLoaders

object TastyMiMa:
  def analyze(
    oldClasspath: Classpath,
    oldClasspathEntry: Classpath.Entry,
    newClasspath: Classpath,
    newClasspathEntry: Classpath.Entry
  ): List[Problem] =
    val oldCtx = Contexts.init(oldClasspath)
    val newCtx = Contexts.init(newClasspath)

    val oldTopSymbols = oldCtx.findSymbolsByClasspathEntry(oldClasspathEntry).toList
    val newTopSymbols = newCtx.findSymbolsByClasspathEntry(newClasspathEntry).toList

    val analyzer = new Analyzer(oldCtx, newCtx)
    analyzer.analyzeTopSymbols(oldTopSymbols, newTopSymbols)
    analyzer.allProblems
  end analyze
end TastyMiMa
