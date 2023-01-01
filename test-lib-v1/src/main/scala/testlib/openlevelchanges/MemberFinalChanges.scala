package testlib.openlevelchanges

sealed abstract class MemberFinalChanges:
  def openToOpen: Int = 1
  def openToFinal: Int = 1
  final def finalToOpen: Int = 1
  final def finalToFinal: Int = 1

  def transitivelyFinalToFinal: Int = 1

  type TypeOpenToOpen = Int
  type TypeOpenToFinal = Int
  final type TypeFinalToOpen = Int
  final type TypeFinalToFinal = Int

  type TypeTransitivelyFinalToFinal = Int
end MemberFinalChanges

final class FinalSubclass extends MemberFinalChanges

class OpenSubclass extends MemberFinalChanges:
  override final def transitivelyFinalToFinal: Int = 1

  override final type TypeTransitivelyFinalToFinal = Int
end OpenSubclass
