package tastymima

import tastyquery.Modifiers.*
import tastyquery.Names.*
import tastyquery.Types.*

import tastymima.intf.{ProblemKind, Problem as IProblem}

final class Problem(val kind: ProblemKind, val path: List[Name], val details: Matchable) extends IProblem:
  import Problem.*

  def this(kind: ProblemKind, path: List[Name]) = this(kind, path, ())

  val pathString: String =
    val s1 = path.mkString(".")
    if path.nonEmpty && path.last.isTypeName && path.last.toTypeName.wrapsObjectName then s1 + "$"
    else s1

  def getKind(): ProblemKind = kind

  def getPathString(): String = pathString

  override def getDescription(): String | Null =
    val superDesc = super.getDescription()

    if details == () then superDesc
    else s"$superDesc: ${detailsString(details)}"
  end getDescription

  private def detailsString(details: Matchable): String = details match
    case details: TypeMappable => details.showBasic
    case details: OpenLevel    => openLevelToString(details)
    case details: BeforeAfter  => s"before: ${detailsString(details.before)}; after: ${detailsString(details.after)}"
    case _                     => details.toString()
  end detailsString

  override def toString(): String =
    s"Problem($kind, $pathString)"
end Problem

object Problem:
  /** Used as `details` for a `Problem` when there is something to show "before" and "after". */
  final case class BeforeAfter(before: Matchable, after: Matchable)

  private def openLevelToString(level: OpenLevel): String = level match
    case OpenLevel.Final  => "final"
    case OpenLevel.Sealed => "sealed"
    case OpenLevel.Closed => "(default)"
    case OpenLevel.Open   => "open"
  end openLevelToString
end Problem
