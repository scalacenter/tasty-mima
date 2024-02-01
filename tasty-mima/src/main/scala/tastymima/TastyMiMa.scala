package tastymima

import java.nio.file.Path
import java.util.List as JList
import java.util.concurrent.Executors

import scala.concurrent.*
import scala.concurrent.duration.Duration

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
    val disableParallel = Option(System.getenv("TASTY_MIMA_PARALLEL")).exists(_.nn.equalsIgnoreCase("false"))
    val ec =
      if disableParallel then ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor().nn)
      else ExecutionContext.global

    given ExecutionContext = ec

    System.err.nn.println("---")
    val start = System.nanoTime()
    val oldCtxAndTopSymbols = Future {
      val oldCtx = Context.initialize(oldClasspath)
      val oldTopSymbols = oldCtx.findSymbolsByClasspathEntry(oldClasspathEntry).toList
      (oldCtx, oldTopSymbols)
    }

    val newCtxAndTopSymbols = Future {
      val newCtx = Context.initialize(newClasspath)
      val newTopSymbols = newCtx.findSymbolsByClasspathEntry(newClasspathEntry).toList
      (newCtx, newTopSymbols)
    }

    val resultFuture =
      for
        (oldCtx, oldTopSymbols) <- oldCtxAndTopSymbols
        (newCtx, newTopSymbols) <- newCtxAndTopSymbols
        _ = System.err.nn.println((System.nanoTime() - start) / 1000L)
        problems <- new Analyzer(config, oldCtx, newCtx).analyzeTopSymbols(oldTopSymbols, newTopSymbols)
      yield
        System.err.nn.println((System.nanoTime() - start) / 1000L)
        problems

    Await.result(resultFuture, Duration.Inf)
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
