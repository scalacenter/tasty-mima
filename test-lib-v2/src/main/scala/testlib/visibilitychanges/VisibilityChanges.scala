package testlib.visibilitychanges

final class VisibilityChanges:
  class Inner:
    // Terms

    private val termPrivateToPrivate: Int = 1
    private[Inner] val termPrivateToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termPrivateToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termPrivateToPackagePrivate: Int = 1
    private[testlib] val termPrivateToOuterPackagePrivate: Int = 1
    protected val termPrivateToProtected: Int = 1
    protected[Inner] val termPrivateToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termPrivateToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termPrivateToPackageProtected: Int = 1
    protected[testlib] val termPrivateToOuterPackageProtected: Int = 1
    val termPrivateToPublic: Int = 1

    private val termInnerQualPrivateToPrivate: Int = 1
    private[Inner] val termInnerQualPrivateToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termInnerQualPrivateToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termInnerQualPrivateToPackagePrivate: Int = 1
    private[testlib] val termInnerQualPrivateToOuterPackagePrivate: Int = 1
    protected val termInnerQualPrivateToProtected: Int = 1
    protected[Inner] val termInnerQualPrivateToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termInnerQualPrivateToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termInnerQualPrivateToPackageProtected: Int = 1
    protected[testlib] val termInnerQualPrivateToOuterPackageProtected: Int = 1
    val termInnerQualPrivateToPublic: Int = 1

    private val termOuterQualPrivateToPrivate: Int = 1
    private[Inner] val termOuterQualPrivateToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termOuterQualPrivateToPackagePrivate: Int = 1
    private[testlib] val termOuterQualPrivateToOuterPackagePrivate: Int = 1
    protected val termOuterQualPrivateToProtected: Int = 1
    protected[Inner] val termOuterQualPrivateToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualPrivateToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termOuterQualPrivateToPackageProtected: Int = 1
    protected[testlib] val termOuterQualPrivateToOuterPackageProtected: Int = 1
    val termOuterQualPrivateToPublic: Int = 1

    private val termPackagePrivateToPrivate: Int = 1
    private[Inner] val termPackagePrivateToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termPackagePrivateToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termPackagePrivateToPackagePrivate: Int = 1
    private[testlib] val termPackagePrivateToOuterPackagePrivate: Int = 1
    protected val termPackagePrivateToProtected: Int = 1
    protected[Inner] val termPackagePrivateToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termPackagePrivateToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termPackagePrivateToPackageProtected: Int = 1
    protected[testlib] val termPackagePrivateToOuterPackageProtected: Int = 1
    val termPackagePrivateToPublic: Int = 1

    private val termOuterPackagePrivateToPrivate: Int = 1
    private[Inner] val termOuterPackagePrivateToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termOuterPackagePrivateToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termOuterPackagePrivateToPackagePrivate: Int = 1
    private[testlib] val termOuterPackagePrivateToOuterPackagePrivate: Int = 1
    protected val termOuterPackagePrivateToProtected: Int = 1
    protected[Inner] val termOuterPackagePrivateToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termOuterPackagePrivateToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termOuterPackagePrivateToPackageProtected: Int = 1
    protected[testlib] val termOuterPackagePrivateToOuterPackageProtected: Int = 1
    val termOuterPackagePrivateToPublic: Int = 1

    private val termProtectedToPrivate: Int = 1
    private[Inner] val termProtectedToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termProtectedToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termProtectedToPackagePrivate: Int = 1
    private[testlib] val termProtectedToOuterPackagePrivate: Int = 1
    protected val termProtectedToProtected: Int = 1
    protected[Inner] val termProtectedToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termProtectedToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termProtectedToPackageProtected: Int = 1
    protected[testlib] val termProtectedToOuterPackageProtected: Int = 1
    val termProtectedToPublic: Int = 1

    private val termInnerQualProtectedToPrivate: Int = 1
    private[Inner] val termInnerQualProtectedToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termInnerQualProtectedToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termInnerQualProtectedToPackagePrivate: Int = 1
    private[testlib] val termInnerQualProtectedToOuterPackagePrivate: Int = 1
    protected val termInnerQualProtectedToProtected: Int = 1
    protected[Inner] val termInnerQualProtectedToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termInnerQualProtectedToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termInnerQualProtectedToPackageProtected: Int = 1
    protected[testlib] val termInnerQualProtectedToOuterPackageProtected: Int = 1
    val termInnerQualProtectedToPublic: Int = 1

    private val termOuterQualProtectedToPrivate: Int = 1
    private[Inner] val termOuterQualProtectedToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termOuterQualProtectedToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termOuterQualProtectedToPackagePrivate: Int = 1
    private[testlib] val termOuterQualProtectedToOuterPackagePrivate: Int = 1
    protected val termOuterQualProtectedToProtected: Int = 1
    protected[Inner] val termOuterQualProtectedToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termOuterQualProtectedToPackageProtected: Int = 1
    protected[testlib] val termOuterQualProtectedToOuterPackageProtected: Int = 1
    val termOuterQualProtectedToPublic: Int = 1

    private val termPackageProtectedToPrivate: Int = 1
    private[Inner] val termPackageProtectedToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termPackageProtectedToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termPackageProtectedToPackagePrivate: Int = 1
    private[testlib] val termPackageProtectedToOuterPackagePrivate: Int = 1
    protected val termPackageProtectedToProtected: Int = 1
    protected[Inner] val termPackageProtectedToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termPackageProtectedToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termPackageProtectedToPackageProtected: Int = 1
    protected[testlib] val termPackageProtectedToOuterPackageProtected: Int = 1
    val termPackageProtectedToPublic: Int = 1

    private val termOuterPackageProtectedToPrivate: Int = 1
    private[Inner] val termOuterPackageProtectedToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termOuterPackageProtectedToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termOuterPackageProtectedToPackagePrivate: Int = 1
    private[testlib] val termOuterPackageProtectedToOuterPackagePrivate: Int = 1
    protected val termOuterPackageProtectedToProtected: Int = 1
    protected[Inner] val termOuterPackageProtectedToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termOuterPackageProtectedToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termOuterPackageProtectedToPackageProtected: Int = 1
    protected[testlib] val termOuterPackageProtectedToOuterPackageProtected: Int = 1
    val termOuterPackageProtectedToPublic: Int = 1

    private val termPublicToPrivate: Int = 1
    private[Inner] val termPublicToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termPublicToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termPublicToPackagePrivate: Int = 1
    private[testlib] val termPublicToOuterPackagePrivate: Int = 1
    protected val termPublicToProtected: Int = 1
    protected[Inner] val termPublicToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termPublicToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termPublicToPackageProtected: Int = 1
    protected[testlib] val termPublicToOuterPackageProtected: Int = 1
    val termPublicToPublic: Int = 1

    // Types

    type typeProtectedToPublic = Int
    private type typePublicToPrivate = Int
    type typePublicToPublic = Int

    // Classes

    class ClassProtectedToPublic
    private class ClassPublicToPrivate
    class ClassPublicToPublic

    // Objects

    object ObjectProtectedToPublic
    private object ObjectPublicToPrivate
    object ObjectPublicToPublic
  end Inner
end VisibilityChanges
