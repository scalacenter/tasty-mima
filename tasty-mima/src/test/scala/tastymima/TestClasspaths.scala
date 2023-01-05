package tastymima

import java.net.URL
import java.nio.file.{FileSystems, Path, Paths}

import tastyquery.Classpaths.*
import tastyquery.jdk.ClasspathLoaders

object TestClasspaths:
  private val TestLibV1ClassPathEnvVar = "TASTYMIMA_TEST_LIBV1_CLASSPATH"
  private val TestLibV2ClassPathEnvVar = "TASTYMIMA_TEST_LIBV2_CLASSPATH"
  private val TastyMiMaClassPathEnvVar = "TASTYMIMA_CLASSPATH"

  private def parsePaths(envVar: String): (List[Path], Path) =
    val stringEntries = System.getenv(envVar).nn.split(';').toList
    val paths: List[Path] =
      for stringEntry <- stringEntries yield stringEntry match
        case s"jrt:/modules/$module/" =>
          FileSystems.getFileSystem(java.net.URI.create("jrt:/")).nn.getPath("modules", module).nn
        case _ =>
          Paths.get(stringEntry).nn
    val entryIndex = paths.indexWhere(_.toString().contains("test-lib"))
    (paths, paths(entryIndex))
  end parsePaths

  val (testLibV1Paths, testLibV1EntryPath) = parsePaths(TestLibV1ClassPathEnvVar)
  val (testLibV2Paths, testLibV2EntryPath) = parsePaths(TestLibV2ClassPathEnvVar)

  private def makeClasspathAndTestLibEntry(paths: List[Path], entryPath: Path): (Classpath, Classpath.Entry) =
    val classpath = ClasspathLoaders.read(paths)
    val testLibEntryIndex = paths.indexOf(entryPath)
    val testLibEntry = classpath.entries(testLibEntryIndex)
    (classpath, testLibEntry)
  end makeClasspathAndTestLibEntry

  val (testLibV1Classpath, testLibV1Entry) = makeClasspathAndTestLibEntry(testLibV1Paths, testLibV1EntryPath)
  val (testLibV2Classpath, testLibV2Entry) = makeClasspathAndTestLibEntry(testLibV2Paths, testLibV2EntryPath)

  def makeFilteredClasspaths(testLibPackageName: String): (Classpath, Classpath.Entry, Classpath, Classpath.Entry) =
    val (v1Classpath, v1Entry) = makeFilteredClasspath(testLibPackageName, testLibV1Classpath, testLibV1Entry)
    val (v2Classpath, v2Entry) = makeFilteredClasspath(testLibPackageName, testLibV2Classpath, testLibV2Entry)
    (v1Classpath, v1Entry, v2Classpath, v2Entry)

  private def makeFilteredClasspath(
    testLibPackageName: String,
    classpath: Classpath,
    testLibEntry: Classpath.Entry
  ): (Classpath, Classpath.Entry) =
    val filteredEntry = Classpath.Entry(testLibEntry.packages.filter { p =>
      p.dotSeparatedName == testLibPackageName || p.dotSeparatedName.startsWith(testLibPackageName + ".")
    })
    val filteredClasspath = Classpath(classpath.entries.map { entry =>
      if entry == testLibEntry then filteredEntry
      else entry
    })
    (filteredClasspath, filteredEntry)
  end makeFilteredClasspath

  val tastyMiMaClasspath: List[URL] =
    val stringEntries = System.getenv(TastyMiMaClassPathEnvVar).nn.split(';').toList
    stringEntries.map(entry => Paths.get(entry).nn.toUri().nn.toURL().nn)
end TestClasspaths
