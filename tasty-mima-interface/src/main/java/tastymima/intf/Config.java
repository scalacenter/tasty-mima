package tastymima.intf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Config {
  private final List<ProblemMatcher> problemFilters;

  private Config(List<ProblemMatcher> problemFilters) {
    this.problemFilters = problemFilters;
  }

  public Config() {
    this(Collections.emptyList());
  }

  public List<ProblemMatcher> getProblemFilters() {
    return problemFilters;
  }

  public Config withMoreProblemFilters(List<ProblemMatcher> additionalFilters) {
    List<ProblemMatcher> newFilters = new ArrayList<ProblemMatcher>(problemFilters.size() + additionalFilters.size());
    newFilters.addAll(problemFilters);
    newFilters.addAll(additionalFilters);
    return withReplacedProblemFiltersInternal(newFilters);
  }

  public Config withReplacedProblemFilters(List<ProblemMatcher> problemFilters) {
    return withReplacedProblemFiltersInternal(new ArrayList<>(problemFilters));
  }

  private Config withReplacedProblemFiltersInternal(List<ProblemMatcher> problemFilters) {
    return new Config(Collections.unmodifiableList(problemFilters));
  }

  @Override
  public boolean equals(Object that) {
    if (!(that instanceof Config))
      return false;
    Config thatConfig = (Config) that;
    return this.getProblemFilters().equals(thatConfig.getProblemFilters());
  }

  @Override
  public int hashCode() {
    int h = 2026047686;
    h = 31 * h + problemFilters.hashCode();
    return h;
  }
}
