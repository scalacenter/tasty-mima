package testlib.visibilitychanges

final class VisibilityChanges:
  class Inner:
    // Terms

    private val termPrivateToPrivate: Int = 1
    private val termPrivateToInnerQualPrivate: Int = 1
    private val termPrivateToOuterQualPrivate: Int = 1
    private val termPrivateToPackagePrivate: Int = 1
    private val termPrivateToOuterPackagePrivate: Int = 1
    private val termPrivateToProtected: Int = 1
    private val termPrivateToInnerQualProtected: Int = 1
    private val termPrivateToOuterQualProtected: Int = 1
    private val termPrivateToPackageProtected: Int = 1
    private val termPrivateToOuterPackageProtected: Int = 1
    private val termPrivateToPublic: Int = 1

    private[Inner] val termInnerQualPrivateToPrivate: Int = 1
    private[Inner] val termInnerQualPrivateToInnerQualPrivate: Int = 1
    private[Inner] val termInnerQualPrivateToOuterQualPrivate: Int = 1
    private[Inner] val termInnerQualPrivateToPackagePrivate: Int = 1
    private[Inner] val termInnerQualPrivateToOuterPackagePrivate: Int = 1
    private[Inner] val termInnerQualPrivateToProtected: Int = 1
    private[Inner] val termInnerQualPrivateToInnerQualProtected: Int = 1
    private[Inner] val termInnerQualPrivateToOuterQualProtected: Int = 1
    private[Inner] val termInnerQualPrivateToPackageProtected: Int = 1
    private[Inner] val termInnerQualPrivateToOuterPackageProtected: Int = 1
    private[Inner] val termInnerQualPrivateToPublic: Int = 1

    private[VisibilityChanges] val termOuterQualPrivateToPrivate: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToInnerQualPrivate: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToOuterQualPrivate: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToPackagePrivate: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToOuterPackagePrivate: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToProtected: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToInnerQualProtected: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToOuterQualProtected: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToPackageProtected: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToOuterPackageProtected: Int = 1
    private[VisibilityChanges] val termOuterQualPrivateToPublic: Int = 1

    private[visibilitychanges] val termPackagePrivateToPrivate: Int = 1
    private[visibilitychanges] val termPackagePrivateToInnerQualPrivate: Int = 1
    private[visibilitychanges] val termPackagePrivateToOuterQualPrivate: Int = 1
    private[visibilitychanges] val termPackagePrivateToPackagePrivate: Int = 1
    private[visibilitychanges] val termPackagePrivateToOuterPackagePrivate: Int = 1
    private[visibilitychanges] val termPackagePrivateToProtected: Int = 1
    private[visibilitychanges] val termPackagePrivateToInnerQualProtected: Int = 1
    private[visibilitychanges] val termPackagePrivateToOuterQualProtected: Int = 1
    private[visibilitychanges] val termPackagePrivateToPackageProtected: Int = 1
    private[visibilitychanges] val termPackagePrivateToOuterPackageProtected: Int = 1
    private[visibilitychanges] val termPackagePrivateToPublic: Int = 1

    private[testlib] val termOuterPackagePrivateToPrivate: Int = 1
    private[testlib] val termOuterPackagePrivateToInnerQualPrivate: Int = 1
    private[testlib] val termOuterPackagePrivateToOuterQualPrivate: Int = 1
    private[testlib] val termOuterPackagePrivateToPackagePrivate: Int = 1
    private[testlib] val termOuterPackagePrivateToOuterPackagePrivate: Int = 1
    private[testlib] val termOuterPackagePrivateToProtected: Int = 1
    private[testlib] val termOuterPackagePrivateToInnerQualProtected: Int = 1
    private[testlib] val termOuterPackagePrivateToOuterQualProtected: Int = 1
    private[testlib] val termOuterPackagePrivateToPackageProtected: Int = 1
    private[testlib] val termOuterPackagePrivateToOuterPackageProtected: Int = 1
    private[testlib] val termOuterPackagePrivateToPublic: Int = 1

    protected val termProtectedToPrivate: Int = 1
    protected val termProtectedToInnerQualPrivate: Int = 1
    protected val termProtectedToOuterQualPrivate: Int = 1
    protected val termProtectedToPackagePrivate: Int = 1
    protected val termProtectedToOuterPackagePrivate: Int = 1
    protected val termProtectedToProtected: Int = 1
    protected val termProtectedToInnerQualProtected: Int = 1
    protected val termProtectedToOuterQualProtected: Int = 1
    protected val termProtectedToPackageProtected: Int = 1
    protected val termProtectedToOuterPackageProtected: Int = 1
    protected val termProtectedToPublic: Int = 1

    protected[Inner] val termInnerQualProtectedToPrivate: Int = 1
    protected[Inner] val termInnerQualProtectedToInnerQualPrivate: Int = 1
    protected[Inner] val termInnerQualProtectedToOuterQualPrivate: Int = 1
    protected[Inner] val termInnerQualProtectedToPackagePrivate: Int = 1
    protected[Inner] val termInnerQualProtectedToOuterPackagePrivate: Int = 1
    protected[Inner] val termInnerQualProtectedToProtected: Int = 1
    protected[Inner] val termInnerQualProtectedToInnerQualProtected: Int = 1
    protected[Inner] val termInnerQualProtectedToOuterQualProtected: Int = 1
    protected[Inner] val termInnerQualProtectedToPackageProtected: Int = 1
    protected[Inner] val termInnerQualProtectedToOuterPackageProtected: Int = 1
    protected[Inner] val termInnerQualProtectedToPublic: Int = 1

    protected[VisibilityChanges] val termOuterQualProtectedToPrivate: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToInnerQualPrivate: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToOuterQualPrivate: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToPackagePrivate: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToOuterPackagePrivate: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToInnerQualProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToOuterQualProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToPackageProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToOuterPackageProtected: Int = 1
    protected[VisibilityChanges] val termOuterQualProtectedToPublic: Int = 1

    protected[visibilitychanges] val termPackageProtectedToPrivate: Int = 1
    protected[visibilitychanges] val termPackageProtectedToInnerQualPrivate: Int = 1
    protected[visibilitychanges] val termPackageProtectedToOuterQualPrivate: Int = 1
    protected[visibilitychanges] val termPackageProtectedToPackagePrivate: Int = 1
    protected[visibilitychanges] val termPackageProtectedToOuterPackagePrivate: Int = 1
    protected[visibilitychanges] val termPackageProtectedToProtected: Int = 1
    protected[visibilitychanges] val termPackageProtectedToInnerQualProtected: Int = 1
    protected[visibilitychanges] val termPackageProtectedToOuterQualProtected: Int = 1
    protected[visibilitychanges] val termPackageProtectedToPackageProtected: Int = 1
    protected[visibilitychanges] val termPackageProtectedToOuterPackageProtected: Int = 1
    protected[visibilitychanges] val termPackageProtectedToPublic: Int = 1

    protected[testlib] val termOuterPackageProtectedToPrivate: Int = 1
    protected[testlib] val termOuterPackageProtectedToInnerQualPrivate: Int = 1
    protected[testlib] val termOuterPackageProtectedToOuterQualPrivate: Int = 1
    protected[testlib] val termOuterPackageProtectedToPackagePrivate: Int = 1
    protected[testlib] val termOuterPackageProtectedToOuterPackagePrivate: Int = 1
    protected[testlib] val termOuterPackageProtectedToProtected: Int = 1
    protected[testlib] val termOuterPackageProtectedToInnerQualProtected: Int = 1
    protected[testlib] val termOuterPackageProtectedToOuterQualProtected: Int = 1
    protected[testlib] val termOuterPackageProtectedToPackageProtected: Int = 1
    protected[testlib] val termOuterPackageProtectedToOuterPackageProtected: Int = 1
    protected[testlib] val termOuterPackageProtectedToPublic: Int = 1

    val termPublicToPrivate: Int = 1
    val termPublicToInnerQualPrivate: Int = 1
    val termPublicToOuterQualPrivate: Int = 1
    val termPublicToPackagePrivate: Int = 1
    val termPublicToOuterPackagePrivate: Int = 1
    val termPublicToProtected: Int = 1
    val termPublicToInnerQualProtected: Int = 1
    val termPublicToOuterQualProtected: Int = 1
    val termPublicToPackageProtected: Int = 1
    val termPublicToOuterPackageProtected: Int = 1
    val termPublicToPublic: Int = 1

    // Types

    protected type typeProtectedToPublic = Int
    type typePublicToPrivate = Int
    type typePublicToPublic = Int

    // Classes

    protected class ClassProtectedToPublic
    class ClassPublicToPrivate
    class ClassPublicToPublic

    // Objects

    protected object ObjectProtectedToPublic
    object ObjectPublicToPrivate
    object ObjectPublicToPublic
  end Inner
end VisibilityChanges
