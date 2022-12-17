package testlib.membertypechanges

final class MemberTypeChanges:
  val valSameType: Int = 1
  var varSameType: Int = 1
  def defSameType: Int = 1
  lazy val lazyValSameType: Int = 1

  val valSubType: Int = 1
  var varSubType: Int = 1
  def defSubType: Int = 1
  lazy val lazyValSubType: Int = 1

  val valOtherType: String = "hello"
  var varOtherType: String = "hello"
  def defOtherType: String = "hello"
  lazy val lazyValOtherType: String = "hello"

  def methodSameResultType(x: Int): Int = x
  def methodSubResultType(x: Int): Int = x
  def methodOtherResultType(x: Int): String = x.toString()

  def methodSameSigSameResultType(x: Int): List[Int] = List(x)
  def methodSameSigSubResultType(x: Int): List[Int] = List(x)
  def methodSameSigOtherResultType(x: Int): List[String] = List(x.toString())
end MemberTypeChanges
