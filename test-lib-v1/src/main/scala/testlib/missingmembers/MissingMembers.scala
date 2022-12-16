package testlib.missingmembers

class MissingMembers:
  val removedVal: Int = 1
  var removedVar: Int = 1
  def removedDef: Int = 1
  object removedModule
  lazy val removedLazyVal: Int = 1

  type removedTypeAlias = Int
  type removedAbstractType
  opaque type removedOpaqueTypeAlias = Int

  val keptVal: Int = 1
  var keptVar: Int = 1
  def keptDef: Int = 1
  object keptModule
  lazy val keptLazyVal: Int = 1

  type keptTypeAlias = Int
  type keptAbstractType
  opaque type keptOpaqueTypeAlias = Int
end MissingMembers
