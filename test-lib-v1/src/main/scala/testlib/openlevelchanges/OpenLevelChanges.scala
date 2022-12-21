package testlib.openlevelchanges

object OpenLevelChanges:
  final class FinalToFinal
  final class FinalToSealed
  final class FinalToDefault
  final class FinalToOpen

  sealed class SealedToFinal
  sealed class SealedToSealed
  sealed class SealedToDefault
  sealed class SealedToOpen

  class DefaultToFinal
  class DefaultToSealed
  class DefaultToDefault
  class DefaultToOpen

  open class OpenToFinal
  open class OpenToSealed
  open class OpenToDefault
  open class OpenToOpen
end OpenLevelChanges
