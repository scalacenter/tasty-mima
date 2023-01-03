package testlib.memberkindchanges

final class TypeMemberKindChanges:
  class ClassToClass
  class ClassToTrait
  class ClassToTypeAlias
  class ClassToAbstractType
  class ClassToOpaqueTypeAlias

  trait TraitToClass
  trait TraitToTrait
  trait TraitToTypeAlias
  trait TraitToAbstractType
  trait TraitToOpaqueTypeAlias

  type TypeAliasToClass = Product
  type TypeAliasToTrait = Product
  type TypeAliasToTypeAlias = Product
  type TypeAliasToAbstractType = Product
  type TypeAliasToOpaqueTypeAlias = Product

  type AbstractTypeToClass <: Product
  type AbstractTypeToTrait <: Product
  type AbstractTypeToTypeAlias <: Product
  type AbstractTypeToAbstractType <: Product
  type AbstractTypeToOpaqueTypeAlias <: Product

  opaque type OpaqueTypeAliasToClass <: Product = Product
  opaque type OpaqueTypeAliasToTrait <: Product = Product
  opaque type OpaqueTypeAliasToTypeAlias <: Product = Product
  opaque type OpaqueTypeAliasToAbstractType <: Product = Product
  opaque type OpaqueTypeAliasToOpaqueTypeAlias <: Product = Product
end TypeMemberKindChanges
