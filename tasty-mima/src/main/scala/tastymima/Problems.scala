package tastymima

import tastyquery.Names.*

object Problems:
  enum Problem:
    case MissingClass(oldClass: ClassInfo)
    case SymbolNotAccessible(info: SymbolInfo)
  end Problem

  sealed class SymbolInfo(val path: List[Name]):
    override def toString(): String =
      path.mkString(".")
  end SymbolInfo

  final class ClassInfo(path: List[Name]) extends SymbolInfo(path)
end Problems
