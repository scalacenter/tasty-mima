package tastymima.intf;

public interface Problem {
  public ProblemKind getKind();
  public String getPathString();

  public default String getDescription() {
    String path = getPathString();
    switch (getKind()) {
      case MissingClass:
        return "The class " + path + " does not have a correspondant in current version";
      case MissingTypeMember:
        return "The type member " + path + " does not have a correspondant in current version";
      case MissingTermMember:
        return "The member " + path + " does not have a correspondant in current version";
      case RestrictedVisibilityChange:
        return "The symbol " + path + " has a more restrictive visibility qualifier in current version";
      case IncompatibleKindChange:
        return "The symbol " + path + " has an incompatible kind in current version";
      case MissingParent:
        return "The class " + path + " is missing a parent in current version";
      case IncompatibleSelfTypeChange:
        return "The class " + path + " has an incompatible self type in current version";
      case RestrictedOpenLevelChange:
        return "The class " + path + " has a more restrictive open level (open, sealed, final) in current version";
      case AbstractClass:
        return "The class " + path + " was concrete but is abstract in current version";
      case FinalMember:
        return "The member " + path + " was open but is final in current version";
      case TypeArgumentCountMismatch:
        return "The class " + path + " does not have the same number of type arguments in current version";
      case IncompatibleTypeChange:
        return "The symbol " + path + " has an incompatible type in current version";
      case NewAbstractMember:
        return "The member " + path + " was concrete or did not exist but is abstract in current version";
      case InternalError:
        return "Internal error while checking symbol " + path;
      default:
        return "Unknown problem " + getKind() + " about symbol " + path;
    }
  }

  public default String getFilterIncantation() {
    return "ProblemMatcher.make(ProblemKind." + getKind() + ", \"" + getPathString() + "\")";
  }
}
