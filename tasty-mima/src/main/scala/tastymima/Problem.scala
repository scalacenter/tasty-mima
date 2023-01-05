package tastymima

import tastyquery.Names.*

import tastymima.intf.{ProblemKind, Problem as IProblem}

final class Problem(val kind: ProblemKind, val path: List[Name]) extends IProblem:
  val pathString: String =
    val s1 = path.mkString(".")
    if path.nonEmpty && path.last.isTypeName && path.last.toTypeName.wrapsObjectName then s1 + "$"
    else s1

  def getKind(): ProblemKind = kind

  def getPathString(): String = pathString

  override def toString(): String =
    s"Problem($kind, $pathString)"
end Problem
