package io.github.liewhite.sqlx
import org.jooq
import scala.deriving.Mirror
import scala.util.Try
import org.jooq.Result
import scala.jdk.CollectionConverters.*

import io.github.liewhite.common.SummonUtils.summonAll

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
          val columnTypes = summonAll[TField, gen.MirroredElemTypes]
          val values      = columnTypes.zip(record.intoList().asScala).map {
            case (col, recItem) => {
              col.dataType.getConverter().from(recItem.asInstanceOf)
            }
          }
          val tuple       = Tuple.fromArray(values.toArray)
          gen.fromProduct(tuple)

        }.toEither
      }
    }
  }
  inline given tuple[A <: Tuple]: FromRecord[A] = {
    new FromRecord[A] {
      override def fromRecord(record: jooq.Record): Either[Throwable, A] = {
        Try {
          val columnTypes = summonAll[TField, A]
          val values      = columnTypes.zip(record.intoList().asScala).map {
            case (col, recItem) => {
              col.dataType.getConverter().from(recItem.asInstanceOf)
            }
          }
          val tp          = Tuple.fromArray(record.intoArray())
          tp.asInstanceOf[A]
        }.toEither
      }
    }
  }
}

extension [R <: jooq.Record](record: R) {
  def as[T](using t: FromRecord[T]): Either[Throwable, T] = {
    t.fromRecord(record)
  }
}

extension [R <: jooq.Record](records: Result[R]) {
  def as[T](using t: FromRecord[T]): Vector[Either[Throwable, T]] = {
    records.asScala.map(i => t.fromRecord(i)).toVector
  }
}
