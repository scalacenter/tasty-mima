package testlib.privatepackages

final class PrivatePackages:
  val publicVal: String = "foo"
  private val privateVal: String = "foo"
  private[PrivatePackages] val classPrivateVal: String = "foo"
  private[privatepackages] val innerPackagePrivateVal: String = "foo"
  private[testlib] val outerPackagePrivateVal: String = "foo"
end PrivatePackages
