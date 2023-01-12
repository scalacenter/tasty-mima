package testlib.internalerrors

abstract class InternalErrors:
  def orTypeInSignature(x: Int | String): Int = 1

  def referenceToRemovedTypeMember(x: Any): Int = 1

  def ok(): Int = 1

  def newAbstractWithOrTypeInSignature(x: Int | String): Int

  def regularTypeProblem(): String = "foo"
end InternalErrors
