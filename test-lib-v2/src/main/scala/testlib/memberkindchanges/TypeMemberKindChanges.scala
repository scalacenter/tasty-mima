package testlib.memberkindchanges

final class TypeMemberKindChanges:
  class ClassToClass
  trait ClassToTrait
  type ClassToTypeAlias = Product
  type ClassToAbstractType <: Product
  opaque type ClassToOpaqueTypeAlias <: Product = Product

  class TraitToClass
  trait TraitToTrait
  type TraitToTypeAlias = Product
  type TraitToAbstractType <: Product
  opaque type TraitToOpaqueTypeAlias <: Product = Product

  class TypeAliasToClass
  trait TypeAliasToTrait
  type TypeAliasToTypeAlias = Product
  type TypeAliasToAbstractType <: Product
  opaque type TypeAliasToOpaqueTypeAlias <: Product = Product

  class AbstractTypeToClass
  trait AbstractTypeToTrait
  type AbstractTypeToTypeAlias = Product
  type AbstractTypeToAbstractType <: Product
  opaque type AbstractTypeToOpaqueTypeAlias <: Product = Product

  class OpaqueTypeAliasToClass
  trait OpaqueTypeAliasToTrait
  type OpaqueTypeAliasToTypeAlias = Product
  type OpaqueTypeAliasToAbstractType <: Product
  opaque type OpaqueTypeAliasToOpaqueTypeAlias <: Product = Product
end TypeMemberKindChanges
