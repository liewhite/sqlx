package io.github.liewhite.sqlx
import org.jooq
import scala.deriving.Mirror
import scala.util.Try
import org.jooq.Result
import scala.jdk.CollectionConverters.*

trait FromRecord[T] {
  def fromRecord(record: jooq.Record): Either[Throwable, T]
}

object FromRecord {
  inline given derived[A <: Product](using
      gen: Mirror.ProductOf[A]
  ): FromRecord[A] = {
    new FromRecord[A] {
      override def fromRecord(record: jooq.Record): Either[Throwable, A] = {
        Try {
          val tuple = Tuple.fromArray(record.intoArray())
          gen.fromProduct(tuple)
        }.toEither
      }
    }
  }
  inline given tuple[A <: Tuple]: FromRecord[A] = {
    new FromRecord[A] {
      override def fromRecord(record: jooq.Record): Either[Throwable, A] = {
        Try {
          val tp = Tuple.fromArray(record.intoArray())
          tp.asInstanceOf[A]
        }.toEither
      }
    }
  }
}

extension [R <: jooq.Record] (record: R) {
    def as[T](using t: FromRecord[T]): Either[Throwable, T] = {
        t.fromRecord(record)
    }
}

extension [R <: jooq.Record] (records: Result[R]) {
    def as[T](using t: FromRecord[T]): Vector[Either[Throwable, T]] = {
        records.asScala.map(i => t.fromRecord(i)).toVector
    }
}
