package testlib.missingmembers

class MissingMembers:
  val keptVal: Int = 1
  var keptVar: Int = 1
  def keptDef: Int = 1
  object keptModule
  lazy val keptLazyVal: Int = 1

  type keptTypeAlias = Int
  type keptAbstractType
  opaque type keptOpaqueTypeAlias = Int
end MissingMembers
