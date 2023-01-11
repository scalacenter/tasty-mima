package testlib.internalerrors

abstract class InternalErrors:
  type RemovedTypeMember

  def orTypeInSignature(x: Int | String): Int = 1

  def referenceToRemovedTypeMember(x: RemovedTypeMember): Int = 1

  def ok(): Int = 1

  def regularTypeProblem(): Int = 1
end InternalErrors
