package testlib.memberkindchanges

final class MemberKindChanges:
  val valToVal: Int = 1
  val valToVar: Int = 1
  val valToModule: Any = Nil
  val valToDef: Int = 1
  val valToLazyVal: Int = 1

  var varToVal: Int = 1
  var varToVar: Int = 1
  var varToModule: Any = Nil
  var varToDef: Int = 1
  var varToLazyVal: Int = 1

  object moduleToVal
  object moduleToVar
  object moduleToModule
  object moduleToDef
  object moduleToLazyVal

  def defToVal: Int = 1
  def defToVar: Int = 1
  def defToModule: Any = Nil
  def defToDef: Int = 1
  def defToLazyVal: Int = 1

  lazy val lazyValToVal: Int = 1
  lazy val lazyValToVar: Int = 1
  lazy val lazyValToModule: Any = Nil
  lazy val lazyValToDef: Int = 1
  lazy val lazyValToLazyVal: Int = 1

  // The presence of the setters does not make transitions to `var`s valid
  def valToVar_=(x: Int): Unit = ()
  def moduleToVar_=(x: Any): Unit = ()
  def lazyValToVar_=(x: Int): Unit = ()
end MemberKindChanges
