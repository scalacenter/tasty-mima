package testlib.classtypeparams

final class ClassTypeParams[A, B <: Product](_a: A, _b: B):
  val a1: A = _a
  val b1: B = _b

  val a2: Any = _a
  val b2: Product = _b

  val a3: A = _a
  val b3: B = _b

  final class Inner[C, D <: A](_c: C, _d: D):
    val c1: C = _c
    val d1: D = _d

    val c2: Any = _c
    val d2: A = _d
    val d3: Any = _d

    val c4: C = _c
    val d4: D = _d
  end Inner

  final class ArgCountMismatch[X, Y](_x: X, _y: Y):
    val x: X = _x
    val y: Y = _y
  end ArgCountMismatch
end ClassTypeParams
