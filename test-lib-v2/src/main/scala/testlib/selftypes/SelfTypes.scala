package testlib.selftypes

trait SelfTypeMono
trait SelfTypePoly[T]

trait OtherSelfTypeMono
trait OtherSelfTypePoly[T]

// Same self types

class ClassSameSelfTypeMono:
  this: SelfTypeMono =>
end ClassSameSelfTypeMono

class ClassSameSelfTypePolyCustom:
  this: SelfTypePoly[Int] =>
end ClassSameSelfTypePolyCustom

class ClassSameSelfTypePolyTParam[A, B]:
  this: SelfTypePoly[A] =>
end ClassSameSelfTypePolyTParam

// Other self types

class ClassOtherSelfTypeMono:
  this: OtherSelfTypeMono =>
end ClassOtherSelfTypeMono

class ClassOtherSelfTypePolyCustom1:
  this: OtherSelfTypePoly[Int] =>
end ClassOtherSelfTypePolyCustom1

class ClassOtherSelfTypePolyCustom2:
  this: SelfTypePoly[String] =>
end ClassOtherSelfTypePolyCustom2

class ClassOtherSelfTypePolyTParam1[A, B]:
  this: OtherSelfTypePoly[A] =>
end ClassOtherSelfTypePolyTParam1

class ClassOtherSelfTypePolyTParam2[A, B]:
  this: SelfTypePoly[B] =>
end ClassOtherSelfTypePolyTParam2

// Other self types for sealed classes

sealed class SealedClassOtherSelfTypeMono:
  this: OtherSelfTypeMono =>
end SealedClassOtherSelfTypeMono

sealed class SealedClassOtherSelfTypePolyCustom:
  this: OtherSelfTypePoly[Int] =>
end SealedClassOtherSelfTypePolyCustom

sealed class SealedClassOtherSelfTypePolyTParam[A, B]:
  this: OtherSelfTypePoly[A] =>
end SealedClassOtherSelfTypePolyTParam

// Adding or removing a self type

class AddSelfType:
  this: SelfTypeMono =>
end AddSelfType

class RemoveSelfType
