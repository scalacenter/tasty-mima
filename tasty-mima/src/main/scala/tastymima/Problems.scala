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

  /** Visibility of a symbol, from the API point of view.
    *
    * Note that any qualified-private with a scope narrower than a package is
    * considered full `private` from the API point of view. Likewise, any
    * qualified-protected with a scope narrower than a package is considered
    * full `protected`.
    */
  enum Visibility:
    case Private
    case PackagePrivate(scope: List[Name])
    case Protected
    case PackageProtected(scope: List[Name])
    case Public
  end Visibility

  enum SymbolKind:
    case Class, Trait, TypeAlias, AbstractTypeMember, OpaqueTypeAlias, TypeParam
    case Module, Method, ValField, VarField, LazyValField
  end SymbolKind

  enum OpenLevel:
    case Final, Sealed, Default, Open
  end OpenLevel
end Problems
