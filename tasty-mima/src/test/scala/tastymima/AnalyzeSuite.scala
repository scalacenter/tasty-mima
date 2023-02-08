package tastymima

import scala.collection.mutable

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*

import tastymima.intf.{Config, ProblemKind, ProblemMatcher}

class AnalyzeSuite extends munit.FunSuite:
  import AnalyzeSuite.*
  import ProblemKind as PK

  def problemsInPackage(packageName: String, config: Config): List[Problem] =
    val fullPackageName = "testlib." + packageName.name
    val classpaths = TestClasspaths.makeFilteredClasspaths(fullPackageName)
    new TastyMiMa(config).analyze(classpaths._1, classpaths._2, classpaths._3, classpaths._4)

  def problemsInPackage(packageName: String): List[Problem] =
    problemsInPackage(packageName, new Config())

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
      PM(PK.MissingClass, "testlib.missingclasses.ClassOnlyInV1"),
      PM(PK.MissingClass, "testlib.missingclasses.ObjectContainer.ClassOnlyInV1")
    )
  }

  test("missing classes - filtered") {
    val filters = java.util.Arrays.asList(
      PM(PK.MissingClass, "testlib.missingclasses.ObjectContainer.ClassOnlyInV1"),
      PM(PK.MissingClass, "testlib.missingclasses.ObjectContainer.ClassOnlyInV2")
    )
    val config = new Config().withMoreProblemFilters(filters).nn
    val problems = problemsInPackage("missingclasses", config)

    assertProblems(problems)(PM(PK.MissingClass, "testlib.missingclasses.ClassOnlyInV1"))
  }

  test("missing members") {
    val problems = problemsInPackage("missingmembers")

    assertProblems(problems)(
      // Terms
      PM(PK.MissingTermMember, "testlib.missingmembers.MissingMembers.removedVal"),
      PM(PK.MissingTermMember, "testlib.missingmembers.MissingMembers.removedVar"),
      PM(PK.MissingTermMember, "testlib.missingmembers.MissingMembers.removedVar_="),
      PM(PK.MissingTermMember, "testlib.missingmembers.MissingMembers.removedDef"),
      PM(PK.MissingTermMember, "testlib.missingmembers.MissingMembers.removedModule"),
      PM(PK.MissingTermMember, "testlib.missingmembers.MissingMembers.removedLazyVal"),
      // Module class
      PM(PK.MissingClass, "testlib.missingmembers.MissingMembers.removedModule$"),
      // Types
      PM(PK.MissingTypeMember, "testlib.missingmembers.MissingMembers.removedTypeAlias"),
      PM(PK.MissingTypeMember, "testlib.missingmembers.MissingMembers.removedAbstractType"),
      PM(PK.MissingTypeMember, "testlib.missingmembers.MissingMembers.removedOpaqueTypeAlias")
    )
  }

  test("inheritance with member removals") {
    val problems = problemsInPackage("inheritancememberremovals")

    assertProblems(problems)(
      PM(PK.MissingTermMember, "testlib.inheritancememberremovals.Child.fieldNotCoveredByParents"),
      PM(PK.MissingTermMember, "testlib.inheritancememberremovals.Child.methodNotCoveredByParents")
    )
  }

  test("member kind changes") {
    val problems = problemsInPackage("memberkindchanges")

    assertProblems(problems)(
      // val to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.valToVar"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.valToDef"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.valToLazyVal"),
      // var to * -> the setters show up
      PM(PK.MissingTermMember, "testlib.memberkindchanges.MemberKindChanges.varToVal_="),
      PM(PK.MissingTermMember, "testlib.memberkindchanges.MemberKindChanges.varToDef_="),
      PM(PK.MissingTermMember, "testlib.memberkindchanges.MemberKindChanges.varToModule_="),
      PM(PK.MissingTermMember, "testlib.memberkindchanges.MemberKindChanges.varToLazyVal_="),
      // module to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.moduleToVal"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.moduleToVar"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.moduleToDef"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.moduleToLazyVal"),
      // lazy val to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.lazyValToVar"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.MemberKindChanges.lazyValToDef"),
      // side effects of module to *
      PM(PK.MissingClass, "testlib.memberkindchanges.MemberKindChanges.moduleToVal$"),
      PM(PK.MissingClass, "testlib.memberkindchanges.MemberKindChanges.moduleToVar$"),
      PM(PK.MissingClass, "testlib.memberkindchanges.MemberKindChanges.moduleToDef$"),
      PM(PK.MissingClass, "testlib.memberkindchanges.MemberKindChanges.moduleToLazyVal$"),
      // class to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.ClassToTrait"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.ClassToTypeAlias"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.ClassToAbstractType"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.ClassToOpaqueTypeAlias"),
      // trait to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TraitToClass"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TraitToTypeAlias"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TraitToAbstractType"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TraitToOpaqueTypeAlias"),
      // type alias to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToClass"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToTrait"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToAbstractType"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.TypeAliasToOpaqueTypeAlias"),
      // abstract type member to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToClass"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToTrait"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToTypeAlias"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.AbstractTypeToOpaqueTypeAlias"),
      // opaque type alias to *
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToClass"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToTrait"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToTypeAlias"),
      PM(PK.IncompatibleKindChange, "testlib.memberkindchanges.TypeMemberKindChanges.OpaqueTypeAliasToAbstractType")
    )
  }

  test("open level changes") {
    val problems = problemsInPackage("openlevelchanges")

    assertProblems(problems)(
      // From default
      PM(PK.RestrictedOpenLevelChange, "testlib.openlevelchanges.OpenLevelChanges.DefaultToFinal"),
      PM(PK.RestrictedOpenLevelChange, "testlib.openlevelchanges.OpenLevelChanges.DefaultToSealed"),
      // From open
      PM(PK.RestrictedOpenLevelChange, "testlib.openlevelchanges.OpenLevelChanges.OpenToFinal"),
      PM(PK.RestrictedOpenLevelChange, "testlib.openlevelchanges.OpenLevelChanges.OpenToSealed"),
      PM(PK.RestrictedOpenLevelChange, "testlib.openlevelchanges.OpenLevelChanges.OpenToDefault"),
      // Term member from open to final
      PM(PK.FinalMember, "testlib.openlevelchanges.MemberFinalChanges.openToFinal"),
      // Type member from open to final
      PM(PK.FinalMember, "testlib.openlevelchanges.MemberFinalChanges.TypeOpenToFinal")
    )
  }

  test("abstract classes") {
    val problems = problemsInPackage("abstractclasses")

    assertProblems(problems)(
      PM(PK.AbstractClass, "testlib.abstractclasses.ConcreteToAbstract"),
      PM(PK.AbstractClass, "testlib.abstractclasses.SealedConcreteToAbstract")
    )
  }

  test("member type changes") {
    val problems = problemsInPackage("membertypechanges")

    assertProblems(problems)(
      // Simple term members
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.MemberTypeChanges.valOtherType"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.MemberTypeChanges.varOtherType"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.MemberTypeChanges.defOtherType"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.MemberTypeChanges.lazyValOtherType"),
      // Method members whose change of result type causes a signature change -> they disappear
      PM(PK.MissingTermMember, "testlib.membertypechanges.MemberTypeChanges.methodSubResultType"),
      PM(PK.MissingTermMember, "testlib.membertypechanges.MemberTypeChanges.methodOtherResultType"),
      // Method members that keep the same signature despite different result types
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.MemberTypeChanges.methodSameSigOtherResultType"),
      // Side effects of changing the type of a var
      PM(PK.MissingTermMember, "testlib.membertypechanges.MemberTypeChanges.varSubType_="),
      PM(PK.MissingTermMember, "testlib.membertypechanges.MemberTypeChanges.varOtherType_="),
      // Members in a (partially) sealed hierarchy
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.SealedHierarchy.defOpenNoOverride"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.SealedHierarchy.defOpenWithOverride"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.SealedHierarchy.defSometimesFinal"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.SealedHierarchy.accessibleProtected"),
      // Type alias
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.TypeMemberTypeChanges.TypeAliasOtherAlias"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.TypeMemberTypeChanges.TypeAliasSubtypeAlias"),
      // Abstract type
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.TypeMemberTypeChanges.AbstractTypeOtherBounds"),
      // Opaque type alias
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.TypeMemberTypeChanges.OpaqueTypeAliasOtherBounds"),
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.TypeMemberTypeChanges.OpaqueTypeAliasOtherErasedAlias"),
      // Polymorphic type alias
      PM(PK.IncompatibleTypeChange, "testlib.membertypechanges.TypeMemberTypeChanges.PolyOpaqueTypeAliasOtherBounds"),
      PM(
        PK.IncompatibleTypeChange,
        "testlib.membertypechanges.TypeMemberTypeChanges.PolyOpaqueTypeAliasSameErasedAlias"
      ),
      PM(
        PK.IncompatibleTypeChange,
        "testlib.membertypechanges.TypeMemberTypeChanges.PolyOpaqueTypeAliasOtherErasedAlias"
      )
    )
  }

  test("class type parameters") {
    val problems = problemsInPackage("classtypeparams")

    assertProblems(problems)(
      // From ClassTypeParams.scala
      PM(PK.IncompatibleTypeChange, "testlib.classtypeparams.ClassTypeParams.a3"),
      PM(PK.IncompatibleTypeChange, "testlib.classtypeparams.ClassTypeParams.b3"),
      PM(PK.IncompatibleTypeChange, "testlib.classtypeparams.ClassTypeParams.Inner.c4"),
      PM(PK.IncompatibleTypeChange, "testlib.classtypeparams.ClassTypeParams.Inner.d4"),
      PM(PK.TypeArgumentCountMismatch, "testlib.classtypeparams.ClassTypeParams.ArgCountMismatch"),
      // From ClassTypeParamBounds.scala
      PM(PK.IncompatibleTypeChange, "testlib.classtypeparams.ClassTypeParamBounds.B"),
      PM(PK.IncompatibleTypeChange, "testlib.classtypeparams.ClassTypeParamBounds.C")
    )
  }

  test("class parents") {
    val problems = problemsInPackage("classparents")

    assertProblems(problems)(
      // Monomorphic parents
      PM(PK.MissingParent, "testlib.classparents.IncompatibleSuperClass"),
      PM(PK.MissingParent, "testlib.classparents.IncompatibleTrait"),
      // Polymorphic parents
      PM(PK.MissingParent, "testlib.classparents.OtherPolyTraitTParam1"),
      PM(PK.MissingParent, "testlib.classparents.OtherPolyTraitTParam2"),
      PM(PK.MissingParent, "testlib.classparents.OtherPolyTraitCustom1"),
      PM(PK.MissingParent, "testlib.classparents.OtherPolyTraitCustom2")
    )
  }

  test("self types") {
    val problems = problemsInPackage("selftypes")

    assertProblems(problems)(
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.ClassOtherSelfTypeMono"),
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.ClassOtherSelfTypePolyCustom1"),
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.ClassOtherSelfTypePolyCustom2"),
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.ClassOtherSelfTypePolyTParam1"),
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.ClassOtherSelfTypePolyTParam2"),
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.AddSelfType"),
      PM(PK.IncompatibleSelfTypeChange, "testlib.selftypes.RemoveSelfType")
    )
  }

  test("type translations") {
    val problems = problemsInPackage("typetranslations")

    assertProblems(problems)(
      // TypeRef
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.namedTypeRefChanged"),
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.typeMemberChanged"),
      // TermRef
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.namedTermRefChanged"),
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.termMemberChanged"),
      // PackageRef
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.packageRefChanged"),
      // ThisType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.thisTypeChanged"),
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.enclosingThisTypeChanged"),
      // SuperType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.superTypeChanged"),
      // ConstantType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.constantTypeChanged"),
      // AppliedType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.appliedTypeTyconChanged"),
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.appliedTypeArgsChanged"),
      // ExprType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.exprTypeChanged"),
      // MethodType + TermParamRef
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.methodTypeChanged"),
      // PolyType + TypeParamRef
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.polyTypeChanged"),
      // TypeLambda + TypeParamRef
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.TypeLambdaChanged"),
      // AnnotatedType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.annotatedTypeChanged"),
      // WildcardTypeBounds
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.wildcardTypeBoundsChanged"),
      // OrType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.orTypeChanged"),
      // AndType
      PM(PK.IncompatibleTypeChange, "testlib.typetranslations.TypeTranslations.Tests.andTypeChanged")
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
      yield PM(
        PK.RestrictedVisibilityChange,
        s"testlib.visibilitychanges.VisibilityChanges.Inner.term${before}To$after"
      )

    val otherExpectedProblems = List(
      // Member types, classes and objects
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.VisibilityChanges.Inner.typePublicToPrivate"),
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.VisibilityChanges.Inner.ClassPublicToPrivate"),
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.VisibilityChanges.Inner.ObjectPublicToPrivate"),
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.VisibilityChanges.Inner.ObjectPublicToPrivate$"),
      // Top-level classes
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.TopClassPublicToPrivate"),
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.TopClassPublicToPackagePrivate"),
      PM(PK.RestrictedVisibilityChange, "testlib.visibilitychanges.TopClassOuterPackagePrivateToPackagePrivate")
    )

    val allExpectedProblems = termExpectedProblems ::: otherExpectedProblems

    assertProblems(problems)(allExpectedProblems*)
  }

  test("new abstract members") {
    val problems = problemsInPackage("newabstractmembers")

    assertProblems(problems)(
      // Actual NewAbstractMember tests
      PM(PK.NewAbstractMember, "testlib.newabstractmembers.NewAbstractMembers.newAbstractVal"),
      PM(PK.NewAbstractMember, "testlib.newabstractmembers.NewAbstractMembers.newAbstractDef"),
      PM(PK.NewAbstractMember, "testlib.newabstractmembers.NewAbstractMembers.oldConcreteVal"),
      PM(PK.NewAbstractMember, "testlib.newabstractmembers.NewAbstractMembers.oldConcreteDef"),
      // For types, this is actually an incompatible *kind* change; a new abstract type is OK
      PM(PK.IncompatibleKindChange, "testlib.newabstractmembers.NewAbstractMembers.OldConcreteType"),
      // Missing class that hides another problem (should not make the rest crash)
      PM(PK.MissingClass, "testlib.newabstractmembers.RemovedOpenSubclass")
    )
  }

  test("artifact-private packages") {
    val artifactPrivatePackages = java.util.Arrays.asList("testlib.privatepackages")
    val config = new Config().withMoreArtifactPrivatePackages(artifactPrivatePackages).nn
    val problems = problemsInPackage("privatepackages", config)

    assertProblems(problems)(
      PM(PK.IncompatibleTypeChange, "testlib.privatepackages.PrivatePackages.publicVal"),
      PM(PK.IncompatibleTypeChange, "testlib.privatepackages.PrivatePackages.outerPackagePrivateVal")
    )
  }

  test("internal errors") {
    val problems = problemsInPackage("internalerrors")

    assertProblems(problems)(
      // Setting up an internal error later on
      PM(PK.MissingTypeMember, "testlib.internalerrors.InternalErrors.RemovedTypeMember"),
      // "Legit" internal error: translating a reference to a type member that does not exist anymore
      PM(PK.InternalError, "testlib.internalerrors.InternalErrors.referenceToRemovedTypeMember"),
      // Assert that we can still detect normal errors afterwards
      PM(PK.NewAbstractMember, "testlib.internalerrors.InternalErrors.newAbstractWithOrTypeInSignature"),
      PM(PK.MissingTermMember, "testlib.internalerrors.InternalErrors.regularTypeProblem")
    )
  }
end AnalyzeSuite

object AnalyzeSuite:
  def PM(kind: ProblemKind, pathString: String): ProblemMatcher =
    ProblemMatcher.make(kind, pathString).nn
end AnalyzeSuite
