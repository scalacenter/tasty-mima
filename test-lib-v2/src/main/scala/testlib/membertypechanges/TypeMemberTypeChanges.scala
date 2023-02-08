package testlib.membertypechanges

final class TypeMemberTypeChanges:
  type TypeAliasSameAlias = Int
  type TypeAliasSubtypeAlias = Int
  type TypeAliasOtherAlias = String

  type AbstractTypeSameBounds >: List[Nothing] <: Seq[AnyVal]
  type AbstractTypeSubBounds >: List[Int] <: Seq[Int]
  type AbstractTypeOtherBounds >: List[Nothing] <: java.io.Serializable

  opaque type OpaqueTypeAliasSameAll >: List[Nothing] <: Seq[AnyVal] = List[Int]
  opaque type OpaqueTypeAliasSubBounds >: List[Int] <: Seq[Int] = List[Int]
  opaque type OpaqueTypeAliasOtherBounds >: List[Nothing] <: java.io.Serializable = List[Int]
  opaque type OpaqueTypeAliasSameErasedAlias >: List[Nothing] <: Seq[AnyVal] = List[AnyVal]
  opaque type OpaqueTypeAliasOtherErasedAlias >: List[Nothing] <: Seq[AnyVal] = Seq[Int]

  opaque type PolyOpaqueTypeAliasSameAll[A] >: List[Nothing] <: Seq[AnyVal] = List[Int]
  opaque type PolyOpaqueTypeAliasSubBounds[A] >: List[Int] <: Seq[Int] = List[Int]
  opaque type PolyOpaqueTypeAliasOtherBounds[A] >: List[Nothing] <: java.io.Serializable = List[Int]
  opaque type PolyOpaqueTypeAliasSameErasedAlias[A] >: List[Nothing] <: Seq[AnyVal] = List[AnyVal]
  opaque type PolyOpaqueTypeAliasOtherErasedAlias[A] >: List[Nothing] <: Seq[AnyVal] = Seq[Int]
end TypeMemberTypeChanges
