package tastymima

import java.nio.file.Path
import java.util.List as JList

import tastyquery.Classpaths.*
import tastyquery.Contexts.*
import tastyquery.jdk.ClasspathLoaders

import tastymima.intf.{Config, Problem as IProblem, TastyMiMa as ITastyMiMa}

final class TastyMiMa(config: Config) extends ITastyMiMa:
  def analyze(
    oldClasspath: Classpath,
    oldClasspathEntry: ClasspathEntry,
    newClasspath: Classpath,
    newClasspathEntry: ClasspathEntry
  ): List[Problem] =
    val oldCtx = Context.initialize(oldClasspath)
    val newCtx = Context.initialize(newClasspath)

    val oldTopSymbols = oldCtx.findSymbolsByClasspathEntry(oldClasspathEntry).toList
    val newTopSymbols = newCtx.findSymbolsByClasspathEntry(newClasspathEntry).toList

    val analyzer = new Analyzer(config, oldCtx, newCtx)
    analyzer.analyzeTopSymbols(oldTopSymbols, newTopSymbols)
    analyzer.allProblems
  end analyze

  def analyze(
    oldClasspath: List[Path],
    oldClasspathEntry: Path,
    newClasspath: List[Path],
    newClasspathEntry: Path
  ): List[Problem] =
    val oldEntryIndex = oldClasspath.indexOf(oldClasspathEntry)
    val newEntryIndex = newClasspath.indexOf(newClasspathEntry)

    if oldEntryIndex < 0 || newEntryIndex < 0 then
      throw IllegalArgumentException("Entries must be elements of their corresponding classpatsh")

    val allDistinctPaths = (oldClasspath ::: newClasspath).distinct
    val pathsToEntries = allDistinctPaths.zip(tastyquery.jdk.ClasspathLoaders.read(allDistinctPaths)).toMap

    val oldTQClasspath = oldClasspath.map(pathsToEntries)
    val oldTQEntry = oldTQClasspath(oldEntryIndex)
    val newTQClasspath = newClasspath.map(pathsToEntries)
    val newTQEntry = newTQClasspath(newEntryIndex)

    analyze(oldTQClasspath, oldTQEntry, newTQClasspath, newTQEntry)
  end analyze

  def analyze(
    oldClasspath: JList[Path],
    oldClasspathEntry: Path,
    newClasspath: JList[Path],
    newClasspathEntry: Path
  ): JList[IProblem] =
    import scala.jdk.CollectionConverters.*

    val problems =
      analyze(oldClasspath.asScala.toList, oldClasspathEntry, newClasspath.asScala.toList, newClasspathEntry)

    new java.util.ArrayList[IProblem](problems.asJava)
  end analyze
end TastyMiMa
