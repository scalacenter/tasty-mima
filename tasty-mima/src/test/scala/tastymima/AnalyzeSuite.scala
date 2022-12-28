package tastymima

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*

import tastymima.Problems.*

class AnalyzeSuite extends munit.FunSuite:
  import AnalyzeSuite.*
  import ProblemMatcher as PM

  def problemsInPackage(packageName: String): List[Problem] =
    val fullPackageName = "testlib." + packageName.name
    val classpaths = TestClasspaths.makeFilteredClasspaths(fullPackageName)
    TastyMiMa.analyze(classpaths._1, classpaths._2, classpaths._3, classpaths._4)

  def assertProblems(using munit.Location)(actualProblems: List[Problem])(
    expectedProblemMatchers: ProblemMatcher*
  ): Unit =
    val remainingMatchers = mutable.ListBuffer.from(expectedProblemMatchers)
    val unexpectedProblems = mutable.ListBuffer.empty[Problem]
    for actualProblem <- actualProblems do
      val matcher = remainingMatchers.find(m => m(actualProblem))
      matcher match
        case Some(m) =>
          remainingMatchers -= m
        case None =>
          unexpectedProblems += actualProblem

    if remainingMatchers.nonEmpty || unexpectedProblems.nonEmpty then
      var msg = "Assertion on problem list failed."
      if remainingMatchers.nonEmpty then msg += remainingMatchers.mkString("\nMissing problems:\n* ", "\n* ", "")
      if unexpectedProblems.nonEmpty then msg += unexpectedProblems.mkString("\nUnexpected problems:\n* ", "\n* ", "")
      fail(msg)
  end assertProblems

  test("missing classes") {
    val problems = problemsInPackage("missingclasses")

    assertProblems(problems)(
      PM.MissingClass("testlib.missingclasses.ClassOnlyInV1"),
      PM.MissingClass("testlib.missingclasses.ObjectContainer.ClassOnlyInV1")
    )
  }

  test("missing members") {
    val problems = problemsInPackage("missingmembers")

    assertProblems(problems)(
      // Terms
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedVal"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedVar"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedVar_="),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedDef"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedModule"),
      PM.MissingTermMember("testlib.missingmembers.MissingMembers.removedLazyVal"),
      // Module class
      PM.MissingClass("testlib.missingmembers.MissingMembers.removedModule$"),
      // Types
      PM.MissingTypeMember("testlib.missingmembers.MissingMembers.removedTypeAlias"),
      PM.MissingTypeMember("testlib.missingmembers.MissingMembers.removedAbstractType"),
      PM.MissingTypeMember("testlib.missingmembers.MissingMembers.removedOpaqueTypeAlias")
    )
  }

  test("inheritance with member removals") {
    val problems = problemsInPackage("inheritancememberremovals")

    assertProblems(problems)(
      PM.MissingTermMember("testlib.inheritancememberremovals.Child.fieldNotCoveredByParents"),
      PM.MissingTermMember("testlib.inheritancememberremovals.Child.methodNotCoveredByParents")
    )
  }

  test("member kind changes") {
    import SymbolKind.*

    val problems = problemsInPackage("memberkindchanges")

    assertProblems(problems)(
      // val to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.valToVar", ValField, VarField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.valToDef", ValField, Method),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.valToLazyVal", ValField, LazyValField),
      // var to * -> the setters show up
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToVal_="),
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToDef_="),
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToModule_="),
      PM.MissingTermMember("testlib.memberkindchanges.MemberKindChanges.varToLazyVal_="),
      // module to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToVal", Module, ValField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToVar", Module, VarField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToDef", Module, Method),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.moduleToLazyVal", Module, LazyValField),
      // lazy val to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.lazyValToVar", LazyValField, VarField),
      PM.IncompatibleKindChange("testlib.memberkindchanges.MemberKindChanges.lazyValToDef", LazyValField, Method),
      // side effects of module to *
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToVal$"),
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToVar$"),
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToDef$"),
      PM.MissingClass("testlib.memberkindchanges.MemberKindChanges.moduleToLazyVal$"),
      // class to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.TypeMemberKindChanges.ClassToTrait", Class, Trait),
      PM.IncompatibleKindChange("testlib.memberkindchanges.TypeMemberKindChanges.ClassToTypeAlias", Class, TypeAlias),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.ClassToAbstractType",
        Class,
        AbstractTypeMember
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.ClassToOpaqueTypeAlias",
        Class,
        OpaqueTypeAlias
      ),
      // trait to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.TypeMemberKindChanges.TraitToClass", Trait, Class),
      PM.IncompatibleKindChange("testlib.memberkindchanges.TypeMemberKindChanges.TraitToTypeAlias", Trait, TypeAlias),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.TraitToAbstractType",
        Trait,
        AbstractTypeMember
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.TraitToOpaqueTypeAlias",
        Trait,
        OpaqueTypeAlias
      ),
      // type alias to *
      PM.IncompatibleKindChange("testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToClass", TypeAlias, Class),
      PM.IncompatibleKindChange("testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToTrait", TypeAlias, Trait),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToAbstractType",
        TypeAlias,
        AbstractTypeMember
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToOpaqueTypeAlias",
        TypeAlias,
        OpaqueTypeAlias
      ),
      // abstract type member to *
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToClass",
        AbstractTypeMember,
        Class
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToTrait",
        AbstractTypeMember,
        Trait
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToTypeAlias",
        AbstractTypeMember,
        TypeAlias
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToOpaqueTypeAlias",
        AbstractTypeMember,
        OpaqueTypeAlias
      ),
      // opaque type alias to *
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToClass",
        OpaqueTypeAlias,
        Class
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToTrait",
        OpaqueTypeAlias,
        Trait
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToTypeAlias",
        OpaqueTypeAlias,
        TypeAlias
      ),
      PM.IncompatibleKindChange(
        "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToAbstractType",
        OpaqueTypeAlias,
        AbstractTypeMember
      )
    )
  }

  test("open level changes") {
    import OpenLevel.*

    val problems = problemsInPackage("openlevelchanges")

    assertProblems(problems)(
      // From default
      PM.RestrictedOpenLevelChange("testlib.openlevelchanges.OpenLevelChanges.DefaultToFinal", Default, Final),
      PM.RestrictedOpenLevelChange("testlib.openlevelchanges.OpenLevelChanges.DefaultToSealed", Default, Sealed),
      // From open
      PM.RestrictedOpenLevelChange("testlib.openlevelchanges.OpenLevelChanges.OpenToFinal", Open, Final),
      PM.RestrictedOpenLevelChange("testlib.openlevelchanges.OpenLevelChanges.OpenToSealed", Open, Sealed),
      PM.RestrictedOpenLevelChange("testlib.openlevelchanges.OpenLevelChanges.OpenToDefault", Open, Default)
    )
  }

  test("member type changes") {
    val problems = problemsInPackage("membertypechanges")

    assertProblems(problems)(
      // Simple term members
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.valOtherType"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.varOtherType"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.defOtherType"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.lazyValOtherType"),
      // Method members whose change of result type causes a signature change -> they disappear
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.methodSubResultType"),
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.methodOtherResultType"),
      // Method members that keep the same signature despite different result types
      PM.IncompatibleTypeChange("testlib.membertypechanges.MemberTypeChanges.methodSameSigOtherResultType"),
      // Side effects of changing the type of a var
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.varSubType_="),
      PM.MissingTermMember("testlib.membertypechanges.MemberTypeChanges.varOtherType_="),
      // Members in a (partially) sealed hierarchy
      PM.IncompatibleTypeChange("testlib.membertypechanges.SealedHierarchy.defOpenNoOverride"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.SealedHierarchy.defOpenWithOverride"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.SealedHierarchy.defSometimesFinal"),
      PM.IncompatibleTypeChange("testlib.membertypechanges.SealedHierarchy.accessibleProtected")
    )
  }

  test("class type parameters") {
    val problems = problemsInPackage("classtypeparams")

    assertProblems(problems)(
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.a3"),
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.b3"),
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.Inner.c4"),
      PM.IncompatibleTypeChange("testlib.classtypeparams.ClassTypeParams.Inner.d4"),
      PM.TypeArgumentCountMismatch("testlib.classtypeparams.ClassTypeParams.ArgCountMismatch")
    )
  }

  test("type translations") {
    val problems = problemsInPackage("typetranslations")

    assertProblems(problems)(
      // TypeRef
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.namedTypeRefChanged"),
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.typeMemberChanged"),
      // TermRef
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.namedTermRefChanged"),
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.termMemberChanged"),
      // PackageRef
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.packageRefChanged"),
      // ThisType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.thisTypeChanged"),
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.enclosingThisTypeChanged"),
      // SuperType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.superTypeChanged"),
      // ConstantType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.constantTypeChanged"),
      // AppliedType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.appliedTypeTyconChanged"),
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.appliedTypeArgsChanged"),
      // ExprType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.exprTypeChanged"),
      // MethodType + TermParamRef
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.methodTypeChanged"),
      // PolyType + TypeParamRef
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.polyTypeChanged"),
      // TypeLambda + TypeParamRef -- translated, but need to check TypeMembers for this to show up
      //PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.typeLambdaChanged"),
      // AnnotatedType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.annotatedTypeChanged"),
      // WildcardTypeBounds
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.wildcardTypeBoundsChanged"),
      // OrType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.orTypeChanged"),
      // AndType
      PM.IncompatibleTypeChange("testlib.typetranslations.TypeTranslations.Tests.andTypeChanged")
    )
  }

  test("visibility changes") {
    val problems = problemsInPackage("visibilitychanges")

    val termPairsWithProblems: List[(String, String)] = List(
      // From package-private
      "PackagePrivate" -> "Private",
      "PackagePrivate" -> "InnerQualPrivate",
      "PackagePrivate" -> "OuterQualPrivate",
      "PackagePrivate" -> "Protected",
      "PackagePrivate" -> "InnerQualProtected",
      "PackagePrivate" -> "OuterQualProtected",
      // From outer-package-private
      "OuterPackagePrivate" -> "Private",
      "OuterPackagePrivate" -> "InnerQualPrivate",
      "OuterPackagePrivate" -> "OuterQualPrivate",
      "OuterPackagePrivate" -> "PackagePrivate",
      "OuterPackagePrivate" -> "Protected",
      "OuterPackagePrivate" -> "InnerQualProtected",
      "OuterPackagePrivate" -> "OuterQualProtected",
      "OuterPackagePrivate" -> "PackageProtected",
      // From protected
      "Protected" -> "Private",
      "Protected" -> "InnerQualPrivate",
      "Protected" -> "OuterQualPrivate",
      "Protected" -> "PackagePrivate",
      "Protected" -> "OuterPackagePrivate",
      // From inner-qual-protected
      "InnerQualProtected" -> "Private",
      "InnerQualProtected" -> "InnerQualPrivate",
      "InnerQualProtected" -> "OuterQualPrivate",
      "InnerQualProtected" -> "PackagePrivate",
      "InnerQualProtected" -> "OuterPackagePrivate",
      // From outer-qual-protected
      "OuterQualProtected" -> "Private",
      "OuterQualProtected" -> "InnerQualPrivate",
      "OuterQualProtected" -> "OuterQualPrivate",
      "OuterQualProtected" -> "PackagePrivate",
      "OuterQualProtected" -> "OuterPackagePrivate",
      // From package-protected
      "PackageProtected" -> "Private",
      "PackageProtected" -> "InnerQualPrivate",
      "PackageProtected" -> "OuterQualPrivate",
      "PackageProtected" -> "PackagePrivate",
      "PackageProtected" -> "OuterPackagePrivate",
      "PackageProtected" -> "Protected",
      "PackageProtected" -> "InnerQualProtected",
      "PackageProtected" -> "OuterQualProtected",
      // From outer-package-protected
      "OuterPackageProtected" -> "Private",
      "OuterPackageProtected" -> "InnerQualPrivate",
      "OuterPackageProtected" -> "OuterQualPrivate",
      "OuterPackageProtected" -> "PackagePrivate",
      "OuterPackageProtected" -> "OuterPackagePrivate",
      "OuterPackageProtected" -> "Protected",
      "OuterPackageProtected" -> "InnerQualProtected",
      "OuterPackageProtected" -> "OuterQualProtected",
      "OuterPackageProtected" -> "PackageProtected",
      // From public
      "Public" -> "Private",
      "Public" -> "InnerQualPrivate",
      "Public" -> "OuterQualPrivate",
      "Public" -> "PackagePrivate",
      "Public" -> "OuterPackagePrivate",
      "Public" -> "Protected",
      "Public" -> "InnerQualProtected",
      "Public" -> "OuterQualProtected",
      "Public" -> "PackageProtected",
      "Public" -> "OuterPackageProtected"
    )

    val termExpectedProblems =
      for (before, after) <- termPairsWithProblems
      yield PM.RestrictedVisibilityChange(s"testlib.visibilitychanges.VisibilityChanges.Inner.term${before}To$after")

    val otherExpectedProblems = List(
      // Member types, classes and objects
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.VisibilityChanges.Inner.typePublicToPrivate"),
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.VisibilityChanges.Inner.ClassPublicToPrivate"),
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.VisibilityChanges.Inner.ObjectPublicToPrivate"),
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.VisibilityChanges.Inner.ObjectPublicToPrivate$"),
      // Top-level classes
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.TopClassPublicToPrivate"),
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.TopClassPublicToPackagePrivate"),
      PM.RestrictedVisibilityChange("testlib.visibilitychanges.TopClassOuterPackagePrivateToPackagePrivate")
    )

    val allExpectedProblems = termExpectedProblems ::: otherExpectedProblems

    assertProblems(problems)(allExpectedProblems*)
  }

  test("new abstract members") {
    val problems = problemsInPackage("newabstractmembers")

    assertProblems(problems)(
      // Actual NewAbstractMember tests
      PM.NewAbstractMember("testlib.newabstractmembers.NewAbstractMembers.newAbstractVal"),
      PM.NewAbstractMember("testlib.newabstractmembers.NewAbstractMembers.newAbstractDef"),
      PM.NewAbstractMember("testlib.newabstractmembers.NewAbstractMembers.oldConcreteVal"),
      PM.NewAbstractMember("testlib.newabstractmembers.NewAbstractMembers.oldConcreteDef"),
      // For types, this is actually an incompatible *kind* change; a new abstract type is OK
      PM.IncompatibleKindChange(
        "testlib.newabstractmembers.NewAbstractMembers.OldConcreteType",
        SymbolKind.TypeAlias,
        SymbolKind.AbstractTypeMember
      ),
      // Missing class that hides another problem (should not make the rest crash)
      PM.MissingClass("testlib.newabstractmembers.RemovedOpenSubclass")
    )
  }
end AnalyzeSuite

object AnalyzeSuite:
  trait ProblemMatcher:
    def apply(problem: Problem): Boolean

  object ProblemMatcher:
    final case class MissingClass(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.MissingClass(info) => info.toString() == fullName
        case _                          => false
    end MissingClass

    final case class MissingTypeMember(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.MissingTypeMember(info) => info.toString() == fullName
        case _                               => false
    end MissingTypeMember

    final case class MissingTermMember(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.MissingTermMember(info) => info.toString() == fullName
        case _                               => false
    end MissingTermMember

    final case class RestrictedVisibilityChange(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.RestrictedVisibilityChange(info, _, _) => info.toString() == fullName
        case _                                              => false
    end RestrictedVisibilityChange

    final case class IncompatibleKindChange(fullName: String, oldKind: SymbolKind, newKind: SymbolKind)
        extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.IncompatibleKindChange(info, `oldKind`, `newKind`) => info.toString() == fullName
        case _                                                          => false
    end IncompatibleKindChange

    final case class RestrictedOpenLevelChange(fullName: String, oldLevel: OpenLevel, newLevel: OpenLevel)
        extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.RestrictedOpenLevelChange(info, `oldLevel`, `newLevel`) => info.toString() == fullName
        case _                                                               => false
    end RestrictedOpenLevelChange

    final case class TypeArgumentCountMismatch(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.TypeArgumentCountMismatch(info) => info.toString() == fullName
        case _                                       => false
    end TypeArgumentCountMismatch

    final case class IncompatibleTypeChange(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.IncompatibleTypeChange(info) => info.toString() == fullName
        case _                                    => false
    end IncompatibleTypeChange

    final case class NewAbstractMember(fullName: String) extends ProblemMatcher:
      def apply(problem: Problem): Boolean = problem match
        case Problem.NewAbstractMember(info) => info.toString() == fullName
        case _                               => false
    end NewAbstractMember
  end ProblemMatcher
end AnalyzeSuite
