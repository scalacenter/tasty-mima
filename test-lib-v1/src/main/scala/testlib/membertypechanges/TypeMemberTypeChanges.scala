package testlib.membertypechanges

final class TypeMemberTypeChanges:
  type TypeAliasSameAlias = Int
  type TypeAliasSubtypeAlias = AnyVal
  type TypeAliasOtherAlias = Int

  type AbstractTypeSameBounds >: List[Nothing] <: Seq[AnyVal]
  type AbstractTypeSubBounds >: List[Nothing] <: Seq[AnyVal]
  type AbstractTypeOtherBounds >: List[Nothing] <: Seq[AnyVal]

  opaque type OpaqueTypeAliasSameAll >: List[Nothing] <: Seq[AnyVal] = List[Int]
  opaque type OpaqueTypeAliasSubBounds >: List[Nothing] <: Seq[AnyVal] = List[Int]
  opaque type OpaqueTypeAliasOtherBounds >: List[Nothing] <: Seq[AnyVal] = List[Int]
  opaque type OpaqueTypeAliasSameErasedAlias >: List[Nothing] <: Seq[AnyVal] = List[Int]
  opaque type OpaqueTypeAliasOtherErasedAlias >: List[Nothing] <: Seq[AnyVal] = List[Int]
end TypeMemberTypeChanges
