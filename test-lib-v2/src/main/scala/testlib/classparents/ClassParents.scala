package testlib.classparents

class SuperClassA
class SuperClassB extends SuperClassA
class SuperClassC

trait SuperTrait extends SuperClassA

trait TraitA
trait TraitB extends TraitA
trait TraitC

trait OtherTrait

final class SameParents extends SuperClassA with TraitA with OtherTrait
final class CompatibleSuperClass extends SuperClassB with TraitA with OtherTrait
final class CompatibleTrait extends SuperClassA with TraitB with OtherTrait
final class CompatibleSuperClassAndTrait extends SuperClassB with OtherTrait with TraitB
final class CompatibleSuperClassViaTrait extends TraitA with SuperTrait with OtherTrait

final class IncompatibleSuperClass extends SuperClassC with TraitA with OtherTrait
final class IncompatibleTrait extends SuperClassA with TraitC with OtherTrait

trait PolyTrait[T]
trait CovPolyTrait[+T]

final class SamePolyTraitTParam[A, B] extends PolyTrait[A] with CovPolyTrait[B]
final class SamePolyTraitCustom extends PolyTrait[Int] with CovPolyTrait[List[Any]]
final class CompatiblePolyTrait extends PolyTrait[Int] with CovPolyTrait[List[Int]]

final class OtherPolyTraitTParam1[A, B] extends PolyTrait[B] with CovPolyTrait[B]
final class OtherPolyTraitTParam2[A, B] extends PolyTrait[A] with CovPolyTrait[A]

final class OtherPolyTraitCustom1 extends PolyTrait[String] with CovPolyTrait[List[Any]]
final class OtherPolyTraitCustom2 extends PolyTrait[Int] with CovPolyTrait[Seq[Any]]
