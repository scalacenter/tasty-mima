package tastymima.intf;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public interface TastyMiMa {
  public List<Problem> analyze(
    List<Path> oldClasspath,
    Path oldClasspathEntry,
    List<Path> newClasspath,
    Path newClasspathEntry
  );

  static TastyMiMa newInstance(URL[] tastyMiMaClasspath, ClassLoader parent, Config config) {
    return ReflectionLoaderImpl.newInstance(tastyMiMaClasspath, parent, config);
  }
}
