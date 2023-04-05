package testlib.openboundaries

sealed trait LocalChildClass:
  def changingPublic: String = "foo"

  protected def changingProtected: String = "foo"
end LocalChildClass

object LocalChildClass:
  final class FinalImpl extends LocalChildClass

  def anonLocalChild(): LocalChildClass =
    new LocalChildClass {}

  def namedLocalChild(): LocalChildClass =
    class NamedLocalChildClass extends LocalChildClass
    new NamedLocalChildClass
end LocalChildClass
