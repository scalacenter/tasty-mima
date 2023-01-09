package tastymima.intf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Config {
  private final List<ProblemMatcher> problemFilters;
  private final List<String> artifactPrivatePackages;

  private Config(List<ProblemMatcher> problemFilters, List<String> artifactPrivatePackages) {
    this.problemFilters = problemFilters;
    this.artifactPrivatePackages = artifactPrivatePackages;
  }

  public Config() {
    this(Collections.emptyList(), Collections.emptyList());
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
    return new Config(Collections.unmodifiableList(problemFilters), artifactPrivatePackages);
  }

  /**
   * List of packages that are considered private to the library being analyzed.
   *
   * Symbols that are private to any of those packages will not be checked for changes by tasty-mima.
   */
  public List<String> getArtifactPrivatePackages() {
    return artifactPrivatePackages;
  }

  public Config withMoreArtifactPrivatePackages(List<String> additionalPackages) {
    List<String> newPackages = new ArrayList<String>(artifactPrivatePackages.size() + additionalPackages.size());
    newPackages.addAll(artifactPrivatePackages);
    newPackages.addAll(additionalPackages);
    return withReplacedArtifactPrivatePackagesInternal(newPackages);
  }

  public Config withReplacedArtifactPrivatePackages(List<String> artifactPrivatePackages) {
    return withReplacedArtifactPrivatePackagesInternal(new ArrayList<>(artifactPrivatePackages));
  }

  private Config withReplacedArtifactPrivatePackagesInternal(List<String> artifactPrivatePackages) {
    return new Config(problemFilters, Collections.unmodifiableList(artifactPrivatePackages));
  }

  @Override
  public boolean equals(Object that) {
    if (!(that instanceof Config))
      return false;
    Config thatConfig = (Config) that;
    return this.getProblemFilters().equals(thatConfig.getProblemFilters())
      && this.getArtifactPrivatePackages().equals(thatConfig.getArtifactPrivatePackages());
  }

  @Override
  public int hashCode() {
    int h = 2026047686;
    h = 31 * h + problemFilters.hashCode();
    h = 31 * h + artifactPrivatePackages.hashCode();
    return h;
  }
}
