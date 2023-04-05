package testlib.openboundaries

enum SomeEnum:
  case Singleton1, Singleton2
  case Parametric(x: Int)

  def changingPublic: Int = 1

  protected def changingProtected: Int = 1
end SomeEnum
