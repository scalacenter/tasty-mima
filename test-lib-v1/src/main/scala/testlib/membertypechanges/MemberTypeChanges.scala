package testlib.membertypechanges

final class MemberTypeChanges:
  val valSameType: Int = 1
  var varSameType: Int = 1
  def defSameType: Int = 1
  lazy val lazyValSameType: Int = 1

  val valSubType: AnyVal = 1
  var varSubType: AnyVal = 1
  def defSubType: AnyVal = 1
  lazy val lazyValSubType: AnyVal = 1

  val valOtherType: Int = 1
  var varOtherType: Int = 1
  def defOtherType: Int = 1
  lazy val lazyValOtherType: Int = 1

  def methodSameResultType(x: Int): Int = x
  def methodSubResultType(x: Int): AnyVal = x
  def methodOtherResultType(x: Int): Int = x

  def methodSameSigSameResultType(x: Int): List[Int] = List(x)
  def methodSameSigSubResultType(x: Int): List[AnyVal] = List(x)
  def methodSameSigOtherResultType(x: Int): List[Int] = List(x)
end MemberTypeChanges
