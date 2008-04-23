package sbinary;
import org.scalacheck._;
import org.scalacheck.Test._;
import Gen._;
import Arbitrary._;
import Prop._;

import scala.collection._;
import Operations._;
import Instances._;

import scalaz.Equal;
import scalaz.Equal._;

object BinaryTests extends Application{
  def test[T](prop : T => Boolean)(implicit arb : Arbitrary[T]) = {
    check(property(prop)).result match {
      case GenException(e) => e.printStackTrace();
      case PropException(_, e) => e.printStackTrace();
      case x => println(x);
    }
  }

  def testBinaryProperties[T](name : String)(implicit 
                               bin : Binary[T], 
                               arb : Arbitrary[T],
                             equal : Equal[T]) = {
    println(name);
    test((x : T) => equal(x, fromByteArray[T](toByteArray(x))))
    false;
  }

  implicit val arbitraryUnit =Arbitrary[Unit](value(() => ()))

  implicit def equalTrees[K, V](implicit equal : Equal[List[(K, V)]]) : Equal[immutable.SortedMap[K, V]] = new Equal[immutable.SortedMap[K, V]]{
    override def apply(x : immutable.SortedMap[K, V], y : immutable.SortedMap[K, V]) = equal(x.toList, y.toList);
  }
     
  implicit def arbitraryMap[K, V](implicit arbK : Arbitrary[K], arbV : Arbitrary[V]) : Arbitrary[immutable.Map[K, V]] =
    Arbitrary(arbitrary[List[(K, V)]].map(x => immutable.Map.empty ++ x))

  implicit def arbitrarySortedMap[K, V](implicit ord : K => Ordered[K], arbK : Arbitrary[K], arbV : Arbitrary[V]) : Arbitrary[immutable.SortedMap[K, V]] =  Arbitrary(arbitrary[List[(K, V)]].map(x => immutable.TreeMap(x :_*)))

  implicit def arbitrarySet[T](implicit arb : Arbitrary[T]) : Arbitrary[immutable.Set[T]] = Arbitrary(arbitrary[List[T]].map((x : List[T]) => immutable.Set(x :_*)));

  implicit def orderedOption[T](opt : Option[T])(implicit ord : T => Ordered[T]) : Ordered[Option[T]] = new Ordered[Option[T]]{
    def compare(that : Option[T]) = (opt, that) match {
      case (None, None) => 0;
      case (None, Some(_)) => -1;
      case (Some(_), None) => 1;
      case (Some(x), Some(y)) => x.compare(y);
    }
  }

  import generic.Generic._;
  trait Foo;

  case class Bar extends Foo;
  case class Baz (string : String) extends Foo;

  implicit val BarIsBinary : Binary[Bar] = asSingleton(Bar())
  implicit val BazIsBinary : Binary[Baz] = asProduct1(Baz)( (x : Baz) => Tuple1(x.string))  
  implicit val FooIsBinary  : Binary[Foo] = asUnion2 (classOf[Bar], classOf[Baz])

  implicit val arbitraryFoo : Arbitrary[Foo] = Arbitrary[Foo](arbitrary[Boolean].flatMap( (bar : Boolean) =>
                            if (bar) value(Bar) else arbitrary[String].map(Baz(_))))
  

  implicit val FooIsEq = EqualA[Foo]
  implicit def setsAreEq[T] = EqualA[immutable.Set[T]]

  sealed abstract class BinaryTree;
  case class Split(left : BinaryTree, right : BinaryTree) extends BinaryTree;
  case class Leaf extends BinaryTree;

  implicit val BinaryTreeIsEq = EqualA[BinaryTree];

  implicit val BinaryTreeIsBinary : Binary[BinaryTree] = lazyBinary({
    implicit val binaryLeaf = asSingleton(Leaf());

    implicit val binarySplit : Binary[Split] = asProduct2((x : BinaryTree, y : BinaryTree) => Split(x, y))((s : Split) => (s.left, s.right));
    asUnion2(classOf[Leaf], classOf[Split]);
  })

  implicit val arbitraryTree : Arbitrary[BinaryTree] = {
    def sizedArbitraryTree(n : Int) : Gen[BinaryTree] = 
      if (n <= 1) value(Leaf());
      else for (i <- choose(1, n - 1);
                left <- sizedArbitraryTree(i);
                right <- sizedArbitraryTree(n - i)
               ) yield (Split(left, right));
    Arbitrary[BinaryTree](sized(sizedArbitraryTree(_ : Int)))
  }

  implicit val arbitraryLong : Arbitrary[Long] = Arbitrary[Long](for (x <- arbitrary[Int]; y <- arbitrary[Int]) yield ((x.toLong << 32) + y));

  // No Arbitrary instances for these. Write some.
  // testBinaryProperties[Short]("Short");
  // testBinaryProperties[Float]("Float");


  println("Primitives");
  testBinaryProperties[Boolean]("Boolean");
  testBinaryProperties[Byte]("Byte");
  testBinaryProperties[Char]("Char");
  testBinaryProperties[Int]("Int");
  testBinaryProperties[Double]("Double");
  testBinaryProperties[Long]("Long");

  println
  testBinaryProperties[Unit]("Unit");

  println
  testBinaryProperties[String]("String")

  println ("Tuples")
  testBinaryProperties[(Int, Int, Int)]("(Int, Int, Int)");
  testBinaryProperties[(String, Int, String)]("(String, Int, String)")
  testBinaryProperties[((Int, (String, Int), Int))]("((Int, (String, Int), Byte, Byte, Int))]");
  testBinaryProperties[(String, String)]("(String, String)")

  println
  println("Options");
  testBinaryProperties[Option[String]]("Option[String]");
  testBinaryProperties[(Option[String], String)]("(Option[String], String)");
  testBinaryProperties[Option[Option[Int]]]("Option[Option[Int]]");
  
  println
  println("Lists");
  testBinaryProperties[List[String]]("List[String]");
  testBinaryProperties[List[(String, Int)]]("List[(String, Int)]");
  testBinaryProperties[List[Option[Int]]]("List[Option[Int]]");
  testBinaryProperties[List[Unit]]("List[Unit]");

  println
  println("immutable.Sets");
  testBinaryProperties[immutable.Set[String]]("immutable.Set[String]");
  testBinaryProperties[immutable.Set[(String, Int)]]("immutable.Set[(String, Int)]");
  testBinaryProperties[immutable.Set[Option[Int]]]("immutable.Set[Option[Int]]");
  testBinaryProperties[immutable.Set[Unit]]("immutable.Set[Unit]");

  println
  println("Arrays");
  testBinaryProperties[String]("Array[String]");
  testBinaryProperties[Array[String]]("Array]Array[String]]");
  testBinaryProperties[List[Int]]("Array[List[Int]]");
  testBinaryProperties[Option[Byte]]("Array[Option[Byte]]");
  testBinaryProperties[Byte]("Array[Byte]");
  testBinaryProperties[(Int, Int)]("Array[(Int, Int)]");

  println
  println("Maps");
  testBinaryProperties[immutable.Map[Int, Int]]("immutable.Map[Int, Int]");
  testBinaryProperties[immutable.Map[Option[String], Int]]("immutable.Map[Option[String], Int]");
  testBinaryProperties[immutable.Map[List[Int], Int]]("immutable.Map[List[Int], String]");

  println("immutable.SortedMaps tests currently disabled");
//  testBinaryProperties[immutable.SortedMap[Int, Int]]("immutable.SortedMap[Int, Int]");
//  testBinaryProperties[immutable.SortedMap[String, Int]]("immutable.SortedMap[String, Int]");
//  testBinaryProperties[immutable.SortedMap[Option[String], Int]]("immutable.SortedMap[Option[String], Int]");
//  testBinaryProperties[immutable.SortedMap[List[Int], Int]]("immutable.SortedMap[List[Int], String]");

  println
  println("Foo (from generic combinators)")
  testBinaryProperties[Foo]("Foo")
  testBinaryProperties[(Foo, Foo)]("(Foo, Foo)")
  testBinaryProperties[Array[Foo]]("Array[Foo]")

  println
  println("BinaryTree");
  testBinaryProperties[BinaryTree]("BinaryTree");
  testBinaryProperties[(BinaryTree, BinaryTree)]("(BinaryTree, BinaryTree)")
}
