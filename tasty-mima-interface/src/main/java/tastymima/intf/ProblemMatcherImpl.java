package tastymima.intf;

import java.util.regex.Pattern;

final class ProblemMatcherImpl {
  public static ProblemMatcher make(ProblemKind kind, String pathStringMatcher) {
    return new SpecificKind(kind, pathStringMatcher);
  }

  public static ProblemMatcher make(String pathStringMatcher) {
    return new AnyKind(pathStringMatcher);
  }

  private static Pattern makePathStringPattern(String pathStringMatcher) {
    StringBuilder regex = new StringBuilder();
    StringBuilder currentChunk = new StringBuilder();
    for (int i = 0; i < pathStringMatcher.length(); i++) {
      switch (pathStringMatcher.charAt(i)) {
        case '*':
          regex.append(Pattern.quote(currentChunk.toString()));
          currentChunk.setLength(0);
          regex.append(".*");
          break;
        case '\\':
          i++;
          currentChunk.append(pathStringMatcher.charAt(i));
          break;
        default:
          currentChunk.append(pathStringMatcher.charAt(i));
      }
    }
    if (currentChunk.length() != 0)
      regex.append(Pattern.quote(currentChunk.toString()));
    return Pattern.compile(regex.toString(), Pattern.DOTALL);
  }

  private static final class SpecificKind implements ProblemMatcher {
    private ProblemKind kind;
    private String pathStringMatcher;
    private Pattern pathStringPattern;

    SpecificKind(ProblemKind kind, String pathStringMatcher) {
      this.kind = kind;
      this.pathStringMatcher = pathStringMatcher;
      this.pathStringPattern = makePathStringPattern(pathStringMatcher);
    }

    public boolean apply(Problem problem) {
      return problem.getKind() == kind && pathStringPattern.matcher(problem.getPathString()).matches();
    }

    public String toString() {
      return "ProblemMatcher.SpecificKind(" + kind + ", " + pathStringMatcher + ")";
    }
  }

  private static final class AnyKind implements ProblemMatcher {
    private String pathStringMatcher;
    private Pattern pathStringPattern;

    AnyKind(String pathStringMatcher) {
      this.pathStringMatcher = pathStringMatcher;
      this.pathStringPattern = makePathStringPattern(pathStringMatcher);
    }

    public boolean apply(Problem problem) {
      return pathStringPattern.matcher(problem.getPathString()).matches();
    }

    public String toString() {
      return "ProblemMatcher.AnyKind(" + pathStringMatcher + ")";
    }
  }
}
