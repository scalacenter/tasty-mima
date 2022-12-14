package tastymima

import tastyquery.Exceptions.*

private[tastymima] object Utils:
  def invalidStructureToOption[A](op: => A): Option[A] =
    try Some(op)
    catch case _: InvalidProgramStructureException => None
end Utils
