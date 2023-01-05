package tastymima.intf;

public interface ProblemMatcher {
  public boolean apply(Problem problem);

  public static ProblemMatcher make(ProblemKind kind, String pathStringMatcher) {
    return ProblemMatcherImpl.make(kind, pathStringMatcher);
  }

  public static ProblemMatcher make(String pathStringMatcher) {
    return ProblemMatcherImpl.make(pathStringMatcher);
  }
}
