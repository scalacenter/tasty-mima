package tastymima

import tastyquery.Names.*

object Problems:
  enum ProblemKind:
    case MissingClass
    case MissingTypeMember
    case MissingTermMember
    case RestrictedVisibilityChange
    case IncompatibleKindChange
    case MissingParent
    case IncompatibleSelfTypeChange
    case RestrictedOpenLevelChange
    case AbstractClass
    case FinalMember
    case TypeArgumentCountMismatch
    case IncompatibleTypeChange
    case NewAbstractMember
  end ProblemKind

  final class Problem(val kind: ProblemKind, val path: List[Name]):
    val pathString: String =
      val s1 = path.mkString(".")
      if path.nonEmpty && path.last.isTypeName && path.last.toTypeName.wrapsObjectName then s1 + "$"
      else s1

    override def toString(): String =
      s"Problem($kind, $pathString)"
  end Problem
end Problems
