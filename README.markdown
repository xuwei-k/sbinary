SBinary
=======

SBinary is a library for describing binary protocols, in the form of mappings between
Scala types and binary formats. It can be used as a robust serialization mechanism for
Scala objects or a way of dealing with existing binary formats found in the wild.

It started out life as a loose port of Haskell's Data.Binary.
It's since evolved a bit from there to take advantage of the features Scala implicits
offer over Haskell type classes, but the core idea has remained the same.

## Getting SBinary

If you are using sbt with Scala 2.10.x or 2.11.x:

```scala
val sbinary = "org.scala-sbt" %% "sbinary" % "0.4.3"
```

## Credits

- SBinary was written by David MacIver. The following is an instruction written by David.

## Usage

The basic entry point to SBinary is the `Protocol` trait.
A Protocol specifies a form of binary IO (although to be honest, you're probably
just going to end up using the standard Java implementation for IO) and a number of Formats.

Binary formats are specified by the following traits:

```scala
trait Reads[T]{
  def reads(in : Input) : T
}

trait Writes[T]{
  def writes(out : Output, value : T)
}

trait Format[T] extends Reads[T] with Writes[T]
```

i.e. a format is something that specifies how to read an element from an input source and
write an element to an output source.

You're not expected to use the reads and writes method directly. Instead Formats are typically
made available implicitly and you use the methods

```scala
def read[T](in : Input)(implicit reader : Reads[T]) = reader.reads(in)
def write[T](out : Output, value : T)(implicit writer : Writes[T]) = writer.writes(out, value)
```

The example interactive sessions are self-contained. Other example code assumes these imports:

```scala
import sbinary._
import DefaultProtocol._
import Operations._
```

The example code is available as a runnable project. If you have checked out the SBinary project
and have sbt installed, run the examples by executing the following in the root directory:

```
$ sbt publishLocal 'project treeExample' run
```

### The Default Protocol

You're expected to define your own protocol for most uses of SBinary in order to serialize your
own types. However for getting started and for simple uses SBinary provides a default protocol
which lets you serialize most standard library types out of the box. It uses Java IO, but all
the types it defines are available in traits if you want to use a different IO mechanism.

So, let's see some examples of how to use this:

```
scala> import sbinary.DefaultProtocol._
import sbinary.DefaultProtocol._

scala> List("foo", "bar", "baz")
res0: List[java.lang.String] = List(foo, bar, baz)

scala> toByteArray(res0)
res1: Array[Byte] = Array(0, 0, 0, 3, 0, 3, 102, 111, 111, 0, 3, 98, 97, 114, 0, 3, 98, 97, 122)

scala> fromByteArrayList[String]
res3: List[String] = List(foo, bar, baz)
```

Straightforward enough. There's also some helper stuff for writing to files:

```
scala> toFile(res0)(new java.io.File("foo.bin"))

scala> fromFile[List[String]](new java.io.File("foo.bin")) 
res7: List[String] = List(foo, bar, baz)
```

Curious fact:

```
scala> fromFile[Array[String]](new java.io.File("foo.bin"))
res8: Array[String] = Array(foo, bar, baz)
```

Note that this is the same file we just wrote as a `ListString` and we've just read it as an `ArrayString`!

In general, SBinary formats aim to be as straightforward as possible. Formats for similar
things tend to be the same. Consequently most of the time you have a fair bit of flexibility
as to whether you write things as the same type you read them as.

### Defining your own protocols

One way to use SBinary would just be to use the default protocol and extend the Format type it provides.
This is perfectly effective, but not really the best way.

The idea is that your usage of SBinary defines a protocol for binary formats. Two different protocols might
define very different formats for the same type, and as long as both are creating formats in the default protocol
there's no way to distinguish between them.

Lest this sound like an obscure theoretical possibility, please consider just how many character encodings
for strings there are (in fact, a very large part of the motivation for the move to protocols in 0.3 was
the fact that I didn't want to force the standard Java UTF format to be used everywhere).

So, the idea is that you define a protocol. For simplicitly, let's make it a single object.
We'll look into how you might modularise it later.

```scala
object MyProtocol extends DefaultProtocol{ }
```

(There is a trait `DefaultProtocol` as well. The `DefaultProtocol` object simply extends it and adds no additional methods).

Great. We have a protocol. It does exactly the same as the default one. But that's a start.

Now, we need something to write to it. Let's consider a simple binary tree type where the leafs are annotated by strings.

```scala
sealed abstract class BT
case class Bin(left: BT, right: BT) extends BT
case class Leaf(label: String) extends BT
```

How could we define a format for this?

Let's start by thinking about how we read this off a stream of bytes.

We first need something to distinguish what we're reading. i.e. we need to know whether we have a `Bin` or a `Leaf`.
So let's start with a marker byte: If it's `0`, we have a `Bin`, otherwise we have a `Leaf`.

So, now we know whether we're reading a `Bin` or a `Leaf`. What do we do now? Easy. If we have a Bin, then we read one BT,
then another. If we have a Leaf, we read a string. We then assemble these in the obvious way:

```scala
object MyProtocol extends DefaultProtocol{
  implicit object BTFormat extends Format[BT]{
    def reads(in: Input) = readByte match {
      case 0 => Bin(reads(in), reads(in))
      case _ => Leaf(readString)
    }

    def writes(out : Output, value : BT) = sys.error("What do I do here?")
  }
}
```

Pretty straightforward. And once we've written that, it becomes obvious what to do for the `write` method:

```scala
object MyProtocol extends DefaultProtocol{
  implicit object BTFormat extends Format[BT] {
    def reads(in: Input) = readByte match {
      case 0 => Bin(reads(in), reads(in))
      case _ => Leaf(readString)
    }

    def writes(out : Output, value : BT) = value match {
      case Bin(left, right) =>
        write[Byte](out, 0)
        writes(out, left)
        writes(out, right)
      case Leaf(label) =>
        write[Byte](out, 1)
        write(out, label)
    }
  }
}
```

Now we can use this just like we would the default protocol:

```scala
scala> import MyProtocol._
import MyProtocol._

scala> Bin(Leaf("foo"), Bin(Leaf("bar"), Leaf("baz")))
res0: Bin = Bin(Leaf(foo),Bin(Leaf(bar),Leaf(baz)))

scala> toByteArrayBT
res2: Array[Byte] = Array(0, 1, 0, 3, 102, 111, 111, 0, 1, 0, 3, 98, 97, 114, 1, 0, 3, 98, 97, 122)

scala> fromByteArrayBT
res3: BT = Bin(Leaf(foo),Bin(Leaf(bar),Leaf(baz)))
```

See, wasn't that easy? Perfectly straightforward to define a binary format, right?

Right?

### Generic format construction

Ok, it wasn't particularly easy. In fact, it was massively verbose. I don't know about you,
but I'd get pretty sick of writing things like the above. And I'd almost certainly make mistakes
(of course, I didn't make any mistakes in SBinary, because I'm amazing. But in theory I might make mistakes.
Particularly if I didn't use a lot of scalacheck tests to make sure I didn't).

So, let's write this a bit differently:

```scala
object MyProtocol extends DefaultProtocol {
  implicit val BTFormat: Format[BT] = lazyFormat(asUnion[BT](
    asProduct2(Bin)(Bin.unapply(_).get),
    wrap[Leaf, String](_.label, Leaf)))
}
```

Which is rather nicer (not to mention shorter) I feel!

Let's unpack how this works through some simpler examples (we're going to do this in the the REPL).

#### Wrapping formats

First let's look at what we did to leaves:

```scala
scala> implicit val LeafFormat = wrap[Leaf, String](_.label, Leaf)
LeafFormat: java.lang.Object with MyProtocol2.Format[Leaf] = sbinary.Generic$$anon$4@1e081c5
```

So the wrap method defines a format for Leaf.

Let's see what it looks like:

```scala
scala> toByteArray(Leaf("foo"))
res9: Array[Byte] = Array(0, 3, 102, 111, 111)

scala> toByteArray("foo")
res10: Array[Byte] = Array(0, 3, 102, 111, 111)
```

The format of the `Leaf` is exactly that of the String it wraps. The two functions which we pass
to wrap are simply used to pack and unpack it from the original format (which was picked up as an implicit argument).

#### Formats for case classes

First let's see how the asProduct stuff works:

```scala
scala> import MyProtocol._
import MyProtocol._

scala> case class Foo(bar : String, baz : Int);
defined class Foo

scala> implicit val Foormat = asProduct2(Foo)(Foo.unapply(_).get)
Foormat: java.lang.Object with MyProtocol2.Format[Foo] = sbinary.Generic$$anon$8@1f5205c

scala> toByteArray(Foo("bar", 3))
res4: Array[Byte] = Array(0, 3, 98, 97, 114, 0, 0, 0, 3)

scala> fromByteArrayFoo
res5: Foo = Foo(bar,3)
```

`asProductN` basically defines binary formats which are concatenations of other formats.
In this instance, Foo is just its components written in order:

```scala
scala> toByteArray("bar")
res6: Array[Byte] = Array(0, 3, 98, 97, 114)

scala> toByteArray(3)
res7: Array[Byte] = Array(0, 0, 0, 3)

scala> res4
res8: Array[Byte] = Array(0, 3, 98, 97, 114, 0, 0, 0, 3)
```

We give `asProduct` methods for assembling and disassembling instances of the type into their component parts.
Conveniently, the `apply` and `unapply` methods generated on case classes do more or less what we want.
This makes it very easy to define formats for case classes.

#### Formats for unions

We frequently have a setup like the following:

```scala
scala> abstract class Bar
defined class Bar

scala> case class Baz(foo : Int) extends Bar
defined class Baz

scala> case class Blib(foo : String) extends Bar
defined class Blib
```

We've seen how to define formats for `Baz` and `Blib`:

```scala
scala> implicit val BazFormat : Format[Baz] = wrap(_.foo, Baz)
BazFormat: MyProtocol2.Format[Baz] = sbinary.Generic$$anon$4@de3c87

scala> implicit val BlibFormat : Format[Blib] = wrap(_.foo, Blib)
BlibFormat: MyProtocol2.Format[Blib] = sbinary.Generic$$anon$4@16b0c24
```

But how do we go from there to a format for `Bar`?

Well, we basically saw how to do this with our hand written binary tree example:
We define a byte tag which says which subtype we're in. So all we need to do to represent this is
specify the formats for the subtypes in order and associate a tag with each. That's what `asUnion` does.

```scala
scala> implicit val BarFormat = asUnion[Bar](BazFormat, BlibFormat)
BarFormat: MyProtocol2.Format[Bar] = sbinary.Generic$$anon$16@12143d8

scala> toByteArrayBar
res11: Array[Byte] = Array(1, 0, 3, 102, 111, 111)

scala> toByteArrayBlib
res12: Array[Byte] = Array(0, 3, 102, 111, 111)

scala> toByteArrayBar
res13: Array[Byte] = Array(0, 0, 0, 0, 3)

scala> toByteArrayBaz
res14: Array[Byte] = Array(0, 0, 0, 3)
```

Note how in each case the Bar format is just a tag prepended to the subformat?

Another important thing to notice: The format depends on the static type of the argument,
not the runtime type. The format written as a Bar is not the same as the format written as a Baz
Because of this you should try to only make formats you actually care about visible,
otherwise type inference can occasionally screw you over. This means that the approach taken in
the binary tree example where the subtypes do not have their own explicit format is generally
a better default than defining a format for each type as in this example.

#### Self referential formats

Suppose we had the following linked list type:

```scala
scala> abstract class LL
defined class LL

scala> case object Nope extends LL
defined module Nope

scala> abstract class Yup(car : Int, cdr : LL) extends LL
defined class Yup
```

Great. This looks like stuff we've seen before. We know how to write a format for it.

```scala
scala> implicit val LLFormat : Format[LL] = asUnion[LL](Nope, asProduct2(Yup)(Yup.unapply(_).get))

scala> toByteArrayLL
res16: Array[Byte] = Array(0)
```

So far so good...

```scala
scala> toByteArray[LL](Yup(1, Nope))
java.lang.NullPointerException at sbinary.Protocol$class.write(protocol.scala:27)
at MyProtocol2$.write(binarytree.scala:7)
at sbinary.Generic$$anon$8.writes(generic.scala:111)
at sbinary.Protocol$class.write(protocol.scala:27)
at MyProtocol2$.write(binarytree.scala:7)
at sbinary.Generic$$anon$16$$anonfun$writes$3.apply(generic.scala:361)
at sbinary.Generic$$anon$16$$anonfun$wr...
```

Oops. What happened here? :-(

Well, the problem is that `asProduct2` takes a bunch of implicit arguments: Two in fact.
One for the format for its first argument, one for the format for its second argument.

Unfortunately the format for its second argument is the format currently being defined!
It hasn't been initialised yet, so we got a null when calling asProduct2 and it all went wrong.

Let's fix it:

```scala
scala> implicit val LLFormat : Format[LL] =lazyFormat(asUnion[LL](Nope, asProduct2(Yup)(Yup.unapply(_).get)))
LLFormat: MyProtocol2.Format[LL] = sbinary.Generic$$anon$5@17ce686

scala> toByteArrayLL 
res18: Array[Byte] = Array(1, 0, 0, 0, 1, 0)
```

`lazyFormat` basically performs magic to make cases like this work. It takes a call by name argument which
will produce a format and returns a format which delegates to that. This means that it's ok to reference the format
currently being defined inside the body because the body is not called during initialisation, only when the format is first used.

#### Putting it all together

Let's look at it again:

```scala
object MyProtocol extends DefaultProtocol {
 implicit val BTFormat : Format[BT] = lazyFormat(asUnion[BT](
   asProduct2(Bin)(Bin.unapply(_).get),
   wrap[Leaf, String](_.label, Leaf) ))
}
```

Hopefully, it should all make sense now. We use `asProduct2` and wrap to define formats for the two subtypes,
use `asUnion` to glue them together and then use `lazyFormat` to allow the format to still refer to itself.

Easy, right?
