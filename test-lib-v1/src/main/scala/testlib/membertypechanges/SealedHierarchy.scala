package testlib.membertypechanges

sealed abstract class SealedHierarchy:
  def defOpenNoOverride: AnyVal = 1
  def defOpenWithOverride: AnyVal = 1
  final def defFinal: AnyVal = 1
  def defSometimesFinal: AnyVal = 1
  def defAllOverridesFinal: AnyVal = 1

  protected def accessibleProtected: Int = 1
end SealedHierarchy

final class FinalSubclass extends SealedHierarchy:
  protected def inaccessibleBecauseFinalProtected: Int = 1

sealed class FullySealedSubclass extends SealedHierarchy:
  override def defOpenWithOverride: AnyVal = 1
  override final def defAllOverridesFinal: AnyVal = 1

  protected def inaccessibleBecauseSealedProtected: Int = 1
end FullySealedSubclass

final class FullySealedSubSubclass extends FullySealedSubclass

sealed class SealedSubclass extends SealedHierarchy:
  override final def defAllOverridesFinal: AnyVal = 1

class OpenSubSubclass1 extends SealedSubclass:
  override final def defOpenWithOverride: AnyVal = 1

open class OpenSubSubclass2 extends SealedSubclass:
  override final def defSometimesFinal: AnyVal = 1

final class FinalSubSubclass extends SealedSubclass
