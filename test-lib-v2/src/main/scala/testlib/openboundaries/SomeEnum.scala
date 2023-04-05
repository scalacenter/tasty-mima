package testlib.openboundaries

enum SomeEnum:
  case Singleton1, Singleton2
  case Parametric(x: Int)

  def changingPublic: String = "foo"

  protected def changingProtected: String = "foo"
end SomeEnum
