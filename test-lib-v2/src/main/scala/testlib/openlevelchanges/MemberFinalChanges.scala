package testlib.openlevelchanges

sealed abstract class MemberFinalChanges:
  def openToOpen: Int = 1
  final def openToFinal: Int = 1
  def finalToOpen: Int = 1
  final def finalToFinal: Int = 1

  final def transitivelyFinalToFinal: Int = 1

  type TypeOpenToOpen = Int
  final type TypeOpenToFinal = Int
  type TypeFinalToOpen = Int
  final type TypeFinalToFinal = Int

  final type TypeTransitivelyFinalToFinal = Int
end MemberFinalChanges

final class FinalSubclass extends MemberFinalChanges

class OpenSubclass extends MemberFinalChanges
