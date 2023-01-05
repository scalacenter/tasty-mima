package testlib.privatepackages

final class PrivatePackages:
  val publicVal: Int = 1
  private val privateVal: Int = 1
  private[PrivatePackages] val classPrivateVal: Int = 1
  private[privatepackages] val innerPackagePrivateVal: Int = 1
  private[testlib] val outerPackagePrivateVal: Int = 1
end PrivatePackages
