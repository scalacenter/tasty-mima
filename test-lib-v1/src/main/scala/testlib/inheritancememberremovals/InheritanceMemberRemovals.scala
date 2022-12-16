package testlib.inheritancememberremovals

class ParentClass:
  val fieldCoveredByParentClass: Int = 1

  def methodCoveredByParentClass(x: Int): Int = x
end ParentClass

trait ParentTrait:
  val fieldCoveredByParentTrait: Int = 1

  def methodCoveredByParentTrait(x: Int): Int = x
end ParentTrait

class Child extends ParentClass with ParentTrait:
  override val fieldCoveredByParentClass: Int = 1
  override val fieldCoveredByParentTrait: Int = 1
  val fieldNotCoveredByParents: Int = 1

  override def methodCoveredByParentClass(x: Int): Int = x
  override def methodCoveredByParentTrait(x: Int): Int = x
  def methodNotCoveredByParents(x: Int): Int = x

  def methodMovedToParentClass(x: Int): Int = x
  def methodMovedToParentTrait(x: Int): Int = x
end Child
