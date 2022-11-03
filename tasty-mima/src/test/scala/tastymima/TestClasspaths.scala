package tastymima

import java.nio.file.{FileSystems, Path, Paths}

import tastyquery.Classpaths.*
import tastyquery.jdk.ClasspathLoaders

object TestClasspaths:
  private val TestLibV1ClassPathEnvVar = "TASTYMIMA_TEST_LIBV1_CLASSPATH"
  private val TestLibV2ClassPathEnvVar = "TASTYMIMA_TEST_LIBV2_CLASSPATH"

  private def makeClasspathAndTestLibEntry(envVar: String): (Classpath, Classpath.Entry) =
    val stringEntries = System.getenv(envVar).nn.split(';').toList
    val paths: List[Path] =
      for stringEntry <- stringEntries yield stringEntry match
        case s"jrt:/modules/$module/" =>
          FileSystems.getFileSystem(java.net.URI.create("jrt:/")).nn.getPath("modules", module).nn
        case _ =>
          Paths.get(stringEntry).nn
    val classpath = ClasspathLoaders.read(paths)
    val testLibEntryIndex = stringEntries.indexWhere(_.contains("test-lib"))
    val testLibEntry = classpath.entries(testLibEntryIndex)
    (classpath, testLibEntry)
  end makeClasspathAndTestLibEntry

  val (testLibV1Classpath, testLibV1Entry) = makeClasspathAndTestLibEntry(TestLibV1ClassPathEnvVar)
  val (testLibV2Classpath, testLibV2Entry) = makeClasspathAndTestLibEntry(TestLibV2ClassPathEnvVar)
end TestClasspaths
