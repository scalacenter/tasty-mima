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

  val newInheritedConcreteVal: Int = 1
  def newInheritedConcreteDef(): Int = 1
end ParentTrait

sealed abstract class NewAbstractMembers extends ParentClass with ParentTrait:
  val newAbstractVal: Int
  def newAbstractDef(): Int

  val oldAbstractVal: Int
  def oldAbstractDef(): Int

  val oldConcreteVal: Int
  def oldConcreteDef(): Int

  val oldInheritedConcreteVal: Int
  def oldInheritedConcreteDef(): Int

  def oldInheritedConcreteAndAbstract(): Int

  def oldAbstractInOpenBoundary(): Int

  val newInheritedConcreteVal: Int
  def newInheritedConcreteDef(): Int

  type NewAbstractType <: Product
  type OldConcreteType <: Product
  type OldAbstractType <: Product
end NewAbstractMembers

abstract class OpenSubclass extends NewAbstractMembers:
  def oldAbstractInOpenBoundary(): Int
end OpenSubclass

trait OpenSubtrait extends NewAbstractMembers:
  def oldAbstractInOpenBoundary(): Int
end OpenSubtrait

abstract class AddedOpenSubclass extends NewAbstractMembers
