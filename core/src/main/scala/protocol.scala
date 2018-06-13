package sbinary;

trait Reads[T] {
  def reads(in: Input): T;
}

trait Writes[T] {
  def writes(out: Output, value: T): Unit;
}

trait Format[T] extends Reads[T] with Writes[T];

trait Protocol {
  implicit object ByteFormat extends Format[Byte] {
    def reads(in: Input) = in.readByte;
    def writes(out: Output, value: Byte) = out.writeByte(value);
  }

  implicit object UnitFormat extends Format[Unit] {
    def reads(in: Input): Unit = {}
    def writes(out: Output, value: Unit): Unit = {}
  }
}

trait CoreProtocol extends Protocol {
  implicit val IntFormat: Format[Int];
  implicit val ShortFormat: Format[Short];
  implicit val LongFormat: Format[Long];
  implicit val BooleanFormat: Format[Boolean];
  implicit val CharFormat: Format[Char];
  implicit val FloatFormat: Format[Float];
  implicit val DoubleFormat: Format[Double];
  implicit val StringFormat: Format[String];
}
