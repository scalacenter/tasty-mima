package testlib.openboundaries

sealed trait LocalChildClass:
  def changingPublic: Int = 1

  protected def changingProtected: Int = 1
end LocalChildClass

object LocalChildClass:
  final class FinalImpl extends LocalChildClass

  def anonLocalChild(): LocalChildClass =
    new LocalChildClass {}

  def namedLocalChild(): LocalChildClass =
    class NamedLocalChildClass extends LocalChildClass
    new NamedLocalChildClass
end LocalChildClass
