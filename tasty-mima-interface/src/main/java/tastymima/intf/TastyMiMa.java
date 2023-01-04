package tastymima.intf;

import java.nio.file.Path;
import java.util.List;

public interface TastyMiMa {
  public List<Problem> analyze(
    List<Path> oldClasspath,
    Path oldClasspathEntry,
    List<Path> newClasspath,
    Path newClasspathEntry
  );
}
