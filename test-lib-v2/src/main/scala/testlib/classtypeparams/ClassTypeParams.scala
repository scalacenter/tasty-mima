package testlib.classtypeparams

final class ClassTypeParams[A2, B <: Product](_a: A2, _b: B):
  val a1: A2 = _a
  val b1: B = _b

  val a2: A2 = _a
  val b2: B = _b

  val a3: B = _b
  val b3: A2 = _a

  final class Inner[C, D <: A2](_c: C, _d: D):
    val c1: C = _c
    val d1: D = _d

    val c2: C = _c
    val d2: D = _d
    val d3: D = _d

    val c4: A2 = _a
    val d4: A2 = _a
  end Inner

  final class ArgCountMismatch[X](_x: X, _y: Any):
    val x: X = _x
    val y: Any = _y
  end ArgCountMismatch
end ClassTypeParams
