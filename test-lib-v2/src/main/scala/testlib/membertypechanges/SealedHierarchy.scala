package testlib.membertypechanges

sealed abstract class SealedHierarchy:
  def defOpenNoOverride: Int = 1
  def defOpenWithOverride: Int = 1
  final def defFinal: Int = 1
  def defSometimesFinal: Int = 1
  def defAllOverridesFinal: Int = 1

  protected def accessibleProtected: String = "foo"
end SealedHierarchy

final class FinalSubclass extends SealedHierarchy:
  protected def inaccessibleBecauseFinalProtected: String = "foo"

sealed class FullySealedSubclass extends SealedHierarchy:
  override def defOpenWithOverride: Int = 1
  override final def defAllOverridesFinal: Int = 1

  protected def inaccessibleBecauseSealedProtected: String = "foo"
end FullySealedSubclass

final class FullySealedSubSubclass extends FullySealedSubclass

sealed class SealedSubclass extends SealedHierarchy:
  override final def defAllOverridesFinal: Int = 1

class OpenSubSubclass1 extends SealedSubclass:
  override final def defOpenWithOverride: Int = 1

open class OpenSubSubclass2 extends SealedSubclass:
  override final def defSometimesFinal: Int = 1

final class FinalSubSubclass extends SealedSubclass
