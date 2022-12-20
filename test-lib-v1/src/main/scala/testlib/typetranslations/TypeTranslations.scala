package testlib.typetranslations

import scala.collection.mutable

final class TypeTranslations:
  import TypeTranslations.*

  type MyTypeMember = String

  abstract class SuperClass:
    type SuperTypeMember = String
    type OtherSuperTypeMember = Int
  end SuperClass

  final class Tests extends SuperClass:
    type MyTypeMember = Int
    val referencedTerm: Container = new ConcreteContainer

    // TypeRef
    val namedTypeRefSame: scala.AnyVal = 1
    val namedTypeRefChanged: scala.AnyVal = 1
    val typeMemberSame: referencedTerm.TypeMember = referencedTerm.termOfTypeMember
    val typeMemberChanged: referencedTerm.TypeMember = referencedTerm.termOfTypeMember

    // TermRef
    val namedTermRefSame: referencedTerm.type = referencedTerm
    val namedTermRefChanged: referencedTerm.type = referencedTerm
    val termMemberSame: referencedTerm.termMember.type = referencedTerm.termMember
    val termMemberChanged: referencedTerm.termMember.type = referencedTerm.termMember

    // PackageRef
    val packageRefSame: scala.collection.immutable.Seq[AnyVal] = Nil
    val packageRefChanged: scala.collection.immutable.Seq[AnyVal] = Nil

    // ThisType
    val thisTypeSame: MyTypeMember = 1
    val thisTypeChanged: MyTypeMember = 1
    val enclosingThisTypeSame: TypeTranslations.this.MyTypeMember = "foo"
    val enclosingThisTypeChanged: TypeTranslations.this.MyTypeMember = "foo"

    // SuperType
    val superTypeSame: Tests.super.SuperTypeMember = "foo"
    val superTypeChanged: Tests.super.SuperTypeMember = "foo"

    // ConstantType
    val constantTypeSame: 42 = 42
    val constantTypeChanged: 42 = 42

    // AppliedType
    val appliedTypeSame: scala.collection.immutable.List[Int] = Nil
    val appliedTypeTyconChanged: scala.collection.immutable.List[Int] = Nil
    val appliedTypeArgsChanged: scala.collection.immutable.List[Int] = Nil

    // ExprType
    val exprTypeSame: (=> String) => String = x => x
    val exprTypeChanged: (=> String) => String = x => x

    // MethodType + TermParamRef
    def methodTypeSame(x: Container, y: Container): x.TypeMember = x.termOfTypeMember
    def methodTypeChanged(x: Container, y: Container): x.TypeMember = x.termOfTypeMember

    // PolyType + TypeParamRef
    def polyTypeSame[A, B <: A](a: A, b: B): (A, B) = (a, b)
    def polyTypeChanged[A, B <: A](a: A, b: B): (A, B) = (a, b)

    // TypeLambda + TypeParamRef
    type TypeLambdaSame[T] = T => T
    type TypeLambdaChanged[T] = T => T

    // AnnotatedType
    val annotatedTypeSame: Int @inline = 1
    val annotatedTypeChanged: Int @inline = 1

    // TODO (not supported by tasty-query subtyping yet): RefinementType's (incl. RecType's), MatchType's

    // WildcardTypeBounds
    val wildcardTypeBoundsSame: mutable.Seq[? <: Product] = mutable.Seq.empty
    val wildcardTypeBoundsChanged: mutable.Seq[? <: Product] = mutable.Seq.empty

    // OrType
    val orTypeSame: Int | String = 1
    val orTypeChanged: Int | String = 1

    // AndType
    val andTypeSame: Product & Serializable = Nil
    val andTypeChanged: Product & Serializable = Nil
  end Tests
end TypeTranslations

object TypeTranslations:
  abstract class Container:
    type TypeMember

    val termMember: Int = 1
    val termOfTypeMember: TypeMember
  end Container

  class ConcreteContainer extends Container:
    type TypeMember = Int

    val termOfTypeMember: TypeMember = 1
  end ConcreteContainer
end TypeTranslations
