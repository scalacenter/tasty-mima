package testlib.openlevelchanges

object OpenLevelChanges:
  final class FinalToFinal
  sealed class FinalToSealed
  class FinalToDefault
  open class FinalToOpen

  final class SealedToFinal
  sealed class SealedToSealed
  class SealedToDefault
  open class SealedToOpen

  final class DefaultToFinal
  sealed class DefaultToSealed
  class DefaultToDefault
  open class DefaultToOpen

  final class OpenToFinal
  sealed class OpenToSealed
  class OpenToDefault
  open class OpenToOpen
end OpenLevelChanges
