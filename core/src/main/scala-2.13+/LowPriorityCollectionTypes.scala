package sbinary

import scala.collection.Factory

trait LowPriorityCollectionTypes extends Generic {
  def canBuildFormat[CC[X] <: Iterable[X], T](implicit bin : Format[T], cbf: Factory[T, CC[T]]) : Format[CC[T]] =
    new LengthEncoded[CC[T], T]{
      def build(length : Int, ts : Iterator[T]) = {
        val builder = cbf.newBuilder
        builder.sizeHint(length)
        builder ++= ts
        if(ts.hasNext) sys.error("Builder did not consume all input.") // no lazy builders allowed
        builder.result()
      }
    }
}
