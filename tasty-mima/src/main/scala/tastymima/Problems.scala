package tastymima

import tastyquery.Names.*

object Problems:
  enum Problem:
    case MissingClass(oldClass: ClassInfo)
    case MissingTypeMember(info: SymbolInfo)
    case MissingTermMember(info: SymbolInfo)
    case RestrictedVisibilityChange(info: SymbolInfo, oldVisibility: Visibility, newVisibility: Visibility)
    case IncompatibleKindChange(info: SymbolInfo, oldKind: SymbolKind, newKind: SymbolKind)
    case RestrictedOpenLevelChange(info: ClassInfo, oldLevel: OpenLevel, newLevel: OpenLevel)
    case TypeArgumentCountMismatch(info: ClassInfo)
    case IncompatibleTypeChange(info: SymbolInfo)
    case NewAbstractMember(info: SymbolInfo)
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
    case PackagePrivate(scopeInfo: SymbolInfo)
    case Protected
    case PackageProtected(scopeInfo: SymbolInfo)
    case Public
  end Visibility

  enum SymbolKind:
    case Class, Trait, TypeAlias, AbstractTypeMember, OpaqueTypeAlias, TypeParam
    case Module, Method, ValField, VarField, LazyValField
  end SymbolKind

  enum OpenLevel:
    case Final, Sealed, Default, Open
  end OpenLevel

  sealed class SymbolInfo(val path: List[Name]):
    override def toString(): String =
      val s1 = path.mkString(".")
      if path.nonEmpty && path.last.isTypeName && path.last.toTypeName.wrapsObjectName then s1 + "$"
      else s1
  end SymbolInfo

  final class ClassInfo(path: List[Name]) extends SymbolInfo(path)
end Problems
