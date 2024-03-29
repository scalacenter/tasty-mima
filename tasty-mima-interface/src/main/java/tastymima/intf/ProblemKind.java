package tastymima.intf;

public enum ProblemKind {
  MissingClass,
  MissingTypeMember,
  MissingTermMember,
  RestrictedVisibilityChange,
  IncompatibleKindChange,
  MissingParent,
  IncompatibleSelfTypeChange,
  RestrictedOpenLevelChange,
  AbstractClass,
  FinalMember,
  TypeArgumentCountMismatch,
  IncompatibleNameChange,
  IncompatibleTypeChange,
  NewAbstractMember,
  InternalError;
}
