package tastymima

import tastyquery.Names.*

object Problems:
  enum Problem:
    case MissingClass(oldClass: ClassInfo)
    case MissingTypeMember(info: SymbolInfo)
    case MissingTermMember(info: SymbolInfo)
    case SymbolNotAccessible(info: SymbolInfo)
    case IncompatibleKindChange(info: SymbolInfo, oldKind: SymbolKind, newKind: SymbolKind)
    case TypeArgumentCountMismatch(info: ClassInfo)
    case IncompatibleTypeChange(info: SymbolInfo)
  end Problem

  enum SymbolKind:
    case Class, TypeAlias, AbstractTypeMember, OpaqueTypeAlias, TypeParam
    case Module, Method, ValField, VarField, LazyValField
  end SymbolKind

  sealed class SymbolInfo(val path: List[Name]):
    override def toString(): String =
      val s1 = path.mkString(".")
      if path.nonEmpty && path.last.isTypeName && path.last.toTypeName.wrapsObjectName then s1 + "$"
      else s1
  end SymbolInfo

  final class ClassInfo(path: List[Name]) extends SymbolInfo(path)
end Problems
