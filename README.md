# TASTy-MiMa

**Disclaimer:** TASTy-MiMa is a young project.
At this point, you are likely going to run into bugs.
Please report issues you find.

TASTy-MiMa (for TASTy Migration Manager) is a tool for identifying TASTy incompatibilities in Scala libraries.
It is pronounced "tasty MEE-ma"

TASTy-MiMa is a direct correspondant of [MiMa](https://github.com/lightbend/mima) for TASTy compatibility instead of binary (classfiles) compatibility.

TASTy-MiMa does *not* replace MiMa.
They are complementary, and should both be used in Scala 3 libraries for comprehensive compatibility checking.

## What it is

TASTy-MiMa can report modifications to the non-private API of a library that may cause *retypechecking errors*.
Retypechecking notably happens when an API is used by an `inline` methods from another library.
Such type errors are usually the consequence of modifications in classes/members signatures.

TASTy-MiMa compares all the `.tasty` files of two released libraries and reports all source of incompatibilities that may lead to retypechecking errors.
TASTy-MiMa provides you, the library maintainer, with a tool that can greatly automate and simplify the process of ensuring the release-to-release compatibility of your libraries.

It is worth mentioning that TASTy compatibility does not imply *source* nor *binary* compatibility (nor vice versa).
For example, adding a public method to a public `final class` is not a source compatible change, although it is a TASTy-compatible change.
However, unlike binary compatibility, TASTy compatibility is a pretty good proxy for our *intuitive* notion of API compatibility.

## Usage

This repository only contains the core functionality of TASTy-MiMa.
In practice, you will want to use a build tool plugin that invokes TASTy-MiMa.

### sbt

For sbt, use [sbt-tasty-mima](https://github.com/scalacenter/sbt-tasty-mima).

For minimal usage, add the following dependency in your `project/plugins.sbt` file:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-tasty-mima" % "<latest-version>")
```

The latest version can be found in [the GitHub Releases page of sbt-tasty-mima](https://github.com/scalacenter/sbt-tasty-mima/releases).

Next, add the following setting in a project for which you want to test TASTy compatibility:

```scala
tastyMiMaPreviousArtifacts += {
  organization.value %% name.value % "<your-library-latest-version>"
}
```

You may also want to add the following configuration:

```scala
tastyMiMaConfig ~= { prevConfig =>
  import java.util.Arrays.asList
  import tastymima.intf._
  prevConfig
    .withMoreArtifactPrivatePackages(asList(
      "my.library.package"
    ))
    .withMoreProblemFilters(asList(
      // add filters here later
    ))
}
```

Run the task `tastyMiMaReportIssues` to see the results.
You can add that task to your CI script; it will fail if it detects any issue.

Every issue is followed by the incantation to add in `withMoreProblemFilters` in order to ignore it.

## Motivation

The rest of this readme details why you probably want to use TASTy-MiMa in your library.

### Compatibility

In general, we can talk about the compatibility between two "environments" A and B.
Assuming B comes *after* A (for some definition of "after" such as version-based), then we can define *backward* and *forward* compatibility:

* *Backward* compatibility: if something "works" with environment A, it also works with environment B.
* *Forward* compatibility: if something works with environment B, it also works with environment A.

By default, and otherwise specified, we refer to *backward* compatibility.

In the Scala 2 ecosystem, there were two kinds of compatibility: *source* and *binary* compatibility.

* Source compatibility: if a program source successfully compiles with environment A, then it also successfully compiles, with the same meaning, with environment B.
* Binary compatibility: if a program's classfiles successfully link with environment A, then they also successfully link, with the same meaning, with environment B.

On the JVM, linking errors materialize as various forms of `LinkageError` at run-time.
In Scala.js and Scala Native, they materialize as error messages at link time, i.e., at the time of producing a `.js` file or an executable.

Scala 3 adds a third kind of compatibility:

* TASTy compatibility: if a program's TASTy files successfully retypecheck with environment A, then they also successfully retypecheck, with the same meaning, with environment B.

As we will see later, "retypechecking" errors typically materialize during macro or `inline def` expansion.

Because of [reasons](https://www.youtube.com/watch?v=2wkEX6MCxJs), it is not practical nor useful to guarantee source compatibility.
The Scala 2 ecosystem is therefore organized around *binary* compatibility.
The Scala 3 ecosystem, however, has to be organized around *both* binary and TASTy compatibility.

### MiMa and the consequences of binary incompatibility

[MiMa](https://github.com/lightbend/mima) is a tool to automatically catch binary incompatibilities between two versions of a library.
MiMa has been instrumental in building a reliable ecosystem for Scala, which avoids [dependency hell](https://en.wikipedia.org/wiki/Dependency_hell).

Oftentimes, as humans not necessarily privy to all the compilation strategies of the Scala compiler, we think that a change will be compatible, although it is not.
A typical example is the addition of a `private var` in a `trait`, e.g., going from

```scala
trait A {
  var x: Int = 1
  def foo(): Int = x
}
```

to

```scala
trait A {
  var x: Int = 1
  private var y: Int = 2
  def foo(): Int = x + y
}
```

When mixing `A` into a class `C`, the compiler needs to create a field for `y` in `C`.
If `C` was compiled against the former version of `A`, but is linked against the latter, linking breaks.

MiMa detects that this change is invalid, and reports it so that it can be fixed before shipping erroneous versions of a library.

### Consequences of TASTy incompatibility

Similarly to binaries, changes in a Scala program can be TASTy incompatible even though we think they are compatible.
For example, adding a `super` call within the body of a trait method, or switching between a `Seq` and a vararg parameter.

Breaking TASTy compatibility in the API of a library `L` can cause issues when retypechecking the library with another library `M` that depends on it, when used from a project `P` that depends on both.
In particular, problems can arise if `M` has `inline def`s that call into the API of `L`.

Here is a concrete example.

```scala
// L.scala
object L {
  def list(xs: Seq[Int]): List[Int] = xs.toList
}
```

```scala
// M.scala
object M {
  inline def foo(): String =
    L.list(Seq(1, 2, 3)).sum.toString
}
```

```scala
// Test.Scala
object Test {
  def main(args: Array[String]): Unit = {
    println(M.foo())
  }
}
```

We first compile everything, and we even verify that it runs:

```
$ cs launch scalac:3.1.2 -- -d bin/ L.scala M.scala Test.scala
$ cs launch scala:3.1.2 -- -cp bin/ Test
6
```

Then, we change `xs: Seq[Int]` into `xs: Int*` in `L.scala`:

```scala
// L.scala
object L {
  def list(xs: Int*): List[Int] = xs.toList
}
```

and recompile only `L`:

```
$ cs launch scalac:3.1.2 -- -d bin L.scala
```

This change is not detected by MiMa, as it is binary compatible.

Finally we try to recompile only `Test`:

```
$ cs launch scalac:3.1.2 -- -cp bin/ -d bin/ Test.scala
-- [E007] Type Mismatch Error: Test.scala:3:17 ---------------------------------
3 |    println(M.foo())
  |            ^^^^^^^
  |            Found:    Seq[Int]
  |            Required: Int
  | This location contains code that was inlined from M.scala:3

longer explanation available when compiling with `-explain`
1 error found
```

This happens even though, from the point of view of `Test`, the change of `L` is both source and binary compatible, because it is not TASTy-compatible.
When inlining the body of `M.foo()` inside `main`, it gets *retypechecked* (but not re-*elaborated*) in the context of `main`.
Retypechecking fails because it is not valid to pass a `Seq[Int]` to a method that expects `Int*` varargs.

For a non-`inline` method, this wouldn't cause any issue, since there would be no reason for the compiler to retypecheck its body.

To guard against this kind of situation, we need an equivalent of MiMa that checks TASTy-compatibility.
This is what TASTy-MiMa does.
