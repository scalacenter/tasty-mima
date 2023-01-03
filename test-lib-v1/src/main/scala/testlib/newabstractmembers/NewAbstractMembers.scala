package testlib.newabstractmembers

abstract class ParentClass:
  def oldInheritedConcreteAndAbstract(): Int
end ParentClass

trait ParentTrait:
  val oldInheritedAbstractVal: Int
  def oldInheritedAbstractDef(): Int

  val oldInheritedConcreteVal: Int = 1
  def oldInheritedConcreteDef(): Int = 1

  def oldInheritedConcreteAndAbstract(): Int = 1
end ParentTrait

sealed abstract class NewAbstractMembers extends ParentClass with ParentTrait:
  val oldAbstractVal: Int
  def oldAbstractDef(): Int

  val oldConcreteVal: Int = 1
  def oldConcreteDef(): Int = 1

  type OldConcreteType = Product
  type OldAbstractType <: Product
end NewAbstractMembers

abstract class OpenSubclass extends NewAbstractMembers:
  def oldAbstractInOpenBoundary(): Int
end OpenSubclass

trait OpenSubtrait extends NewAbstractMembers:
  def oldAbstractInOpenBoundary(): Int
end OpenSubtrait

abstract class RemovedOpenSubclass extends NewAbstractMembers
