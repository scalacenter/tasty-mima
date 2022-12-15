package tastymima

import tastyquery.Exceptions.*

private[tastymima] object Utils:
  def invalidStructureToOption[A](op: => A): Option[A] =
    try Some(op)
    catch case _: InvalidProgramStructureException => None

  def memberNotFoundToOption[A](op: => A): Option[A] =
    try Some(op)
    catch case _: MemberNotFoundException => None
end Utils
