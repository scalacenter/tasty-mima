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
    val otherReferencedTerm: Container = new ConcreteContainer

    // TypeRef
    val namedTypeRefSame: scala.AnyVal = 1
    val namedTypeRefChanged: scala.Product = (1, 2)
    val typeMemberSame: referencedTerm.TypeMember = referencedTerm.termOfTypeMember
    val typeMemberChanged: otherReferencedTerm.TypeMember = otherReferencedTerm.termOfTypeMember

    // TermRef
    val namedTermRefSame: referencedTerm.type = referencedTerm
    val namedTermRefChanged: otherReferencedTerm.type = otherReferencedTerm
    val termMemberSame: referencedTerm.termMember.type = referencedTerm.termMember
    val termMemberChanged: otherReferencedTerm.termMember.type = otherReferencedTerm.termMember

    // PackageRef
    val packageRefSame: scala.collection.immutable.Seq[AnyVal] = Nil
    val packageRefChanged: scala.collection.mutable.Seq[AnyVal] = scala.collection.mutable.ListBuffer.empty

    // ThisType
    val thisTypeSame: MyTypeMember = 1
    val thisTypeChanged: TypeTranslations.this.MyTypeMember = "foo"
    val enclosingThisTypeSame: TypeTranslations.this.MyTypeMember = "foo"
    val enclosingThisTypeChanged: MyTypeMember = 1

    // SuperType
    val superTypeSame: Tests.super.SuperTypeMember = "foo"
    val superTypeChanged: Tests.super.OtherSuperTypeMember = 1

    // ConstantType
    val constantTypeSame: 42 = 42
    val constantTypeChanged: 24 = 24

    // AppliedType
    val appliedTypeSame: scala.collection.immutable.List[Int] = Nil
    val appliedTypeTyconChanged: scala.collection.immutable.IndexedSeq[Int] = Vector.empty
    val appliedTypeArgsChanged: scala.collection.immutable.List[String] = Nil

    // ByNameType
    val byNameTypeSame: (=> String) => String = x => x
    val byNameTypeChanged: (=> Int) => String = x => x.toString()

    // MethodType
    def methodTypeSame(y: Container, x: Container): y.TypeMember = y.termOfTypeMember
    def methodTypeChanged(y: Container, x: Container): x.TypeMember = x.termOfTypeMember

    // PolyType
    def polyTypeSame[B, A <: B](a: B, b: A): (B, A) = (a, b)
    def polyTypeChanged[B, A <: B](a: A, b: B): (A, B) = (a, b)

    // TypeLambda + TypeParamRef
    type TypeLambdaSame[U] = U => U
    type TypeLambdaChanged[T] = T => (T, T)

    // AnnotatedType
    val annotatedTypeSame: Int @inline = 1
    val annotatedTypeChanged: String @inline = "foo"

    // TypeRefinement
    def typeRefinementTypeSame: Container { type TypeMember = Int } = ???
    def typeRefinementTypeChanged: Container { type TypeMember = String } = ???

    // TermRefinement
    def termRefinementTypeSame: Container { val termMember: ::[Int] } = ???
    def termRefinementTypeChanged: Container { val termMember: ::[String] } = ???

    // RecType + RecThis
    def recTypeSame: Container { type Foo = TypeMember } = ???
    def recTypeChanged: Container { type Foo = termMember.type } = ???

    // MatchType
    type MatchTypeSame[T] = T match
      case Int     => String
      case List[t] => t
    type MatchTypeChanged[T] = T match
      case Int     => String
      case List[t] => Int

    // WildcardTypeBounds
    val wildcardTypeBoundsSame: mutable.Seq[? <: Product] = mutable.Seq.empty
    val wildcardTypeBoundsChanged: mutable.Seq[? <: List[Any]] = mutable.Seq.empty

    // OrType
    val orTypeSame: Int | String = 1
    val orTypeChanged: Int | Product = 1

    // AndType
    val andTypeSame: Product & Serializable = Nil
    val andTypeChanged: Product & Seq[Any] = Nil
  end Tests
end TypeTranslations

object TypeTranslations:
  abstract class Container:
    type TypeMember

    val termMember: Product
    val termOfTypeMember: TypeMember
  end Container

  class ConcreteContainer extends Container:
    type TypeMember = Int

    val termMember: Product = Some(1)
    val termOfTypeMember: TypeMember = 1
  end ConcreteContainer
end TypeTranslations
