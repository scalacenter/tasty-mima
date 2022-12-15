package testlib.memberkindchanges

class MemberKindChanges:
  val valToVal: Int = 1
  var valToVar: Int = 1
  object valToModule
  def valToDef: Int = 1
  lazy val valToLazyVal: Int = 1

  val varToVal: Int = 1
  var varToVar: Int = 1
  object varToModule
  def varToDef: Int = 1
  lazy val varToLazyVal: Int = 1

  val moduleToVal: Any = Nil
  var moduleToVar: Any = Nil
  object moduleToModule
  def moduleToDef: Any = Nil
  lazy val moduleToLazyVal: Any = Nil

  val defToVal: Int = 1
  var defToVar: Int = 1
  object defToModule
  def defToDef: Int = 1
  lazy val defToLazyVal: Int = 1

  val lazyValToVal: Int = 1
  var lazyValToVar: Int = 1
  object lazyValToModule
  def lazyValToDef: Int = 1
  lazy val lazyValToLazyVal: Int = 1
end MemberKindChanges
