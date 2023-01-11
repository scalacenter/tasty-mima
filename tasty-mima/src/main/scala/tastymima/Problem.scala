package tastymima

import tastyquery.Names.*

import tastymima.intf.{ProblemKind, Problem as IProblem}

final class Problem(val kind: ProblemKind, val path: List[Name], val details: Matchable) extends IProblem:
  def this(kind: ProblemKind, path: List[Name]) = this(kind, path, ())

  val pathString: String =
    val s1 = path.mkString(".")
    if path.nonEmpty && path.last.isTypeName && path.last.toTypeName.wrapsObjectName then s1 + "$"
    else s1

  def getKind(): ProblemKind = kind

  def getPathString(): String = pathString

  override def getDescription(): String | Null =
    (kind, details) match
      case (ProblemKind.InternalError, error: Throwable) =>
        s"${super.getDescription()}: $error"
      case _ =>
        super.getDescription()
  end getDescription

  override def toString(): String =
    s"Problem($kind, $pathString)"
end Problem
