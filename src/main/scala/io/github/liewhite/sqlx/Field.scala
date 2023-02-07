package io.github.liewhite.sqlx

import java.sql.ResultSet
import java.sql.PreparedStatement
import java.sql.SQLException
import org.jooq.*
import org.jooq
import org.jooq.impl.BuiltInDataType
import org.jooq.impl.SQLDataType
import java.time.LocalDateTime
import java.time.ZonedDateTime
import scala.deriving.Mirror
import java.{util => ju}
import java.lang
import java.time.Instant
import java.time.ZoneId
import java.math.BigInteger
import io.github.liewhite.sqlx.annotation.Precision

case class Field[T](
    index: Int,
    modelName: String,
    // scala case class field name
    fieldName: String,
    // database table name
    primaryKey: Boolean,
    colName: String,
    unique: Boolean,
    default: Option[Any],
    length: Option[Int],
    precision: Option[Precision],
    t: TField[T]) {
  def field: jooq.Field[Object] = {
    jooq.impl.DSL.field(fullColName)
  }

  def fullColName:String = Vector(modelName, colName).mkString(".")
  def getValue[A <: Product: Mirror.ProductOf](o: A): T = {
    val value = Tuple.fromProductTyped(o).toList.apply(index).asInstanceOf[T]
    value
  }

  def uniqueKeyName: String      = "uk:" + colName
  def getDataType: DataType[Any] = {
    var datatype = t.dataType.asInstanceOf[DataType[Any]]
    if (primaryKey) {
      datatype = datatype.nullable(false).identity(true)
    } else {
      datatype = datatype.nullable(false)
      if (default.isDefined) {
        datatype = datatype.defaultValue(default.get.asInstanceOf[Any])
      }
    }
    length.foreach(l => {
      datatype = datatype.length(l)
    })
    precision.foreach(p => {
      datatype = datatype.precision(p.precision,p.scale)
    })

    datatype
  }
}

trait TField[T] {
  // option type with true
  def nullable: Boolean = false
  // jooq datatype
  def dataType: DataType[T]
}

object TField {
  given [T](using t: TField[T]): TField[Option[T]] with {
    override def nullable: Boolean = true
    def dataType: DataType[Option[T]] = t.dataType.nullable(true).asInstanceOf
  }

  given TField[Int] with {
    def dataType: DataType[Int] = SQLDataType.INTEGER.asConvertedDataType(new Converter[Integer, Int]{

      override def from(databaseObject: Integer): Int = databaseObject

      override def to(userObject: Int): Integer = userObject

      override def fromType(): Class[Integer] = classOf[Integer]

      override def toType(): Class[Int] = classOf[Int]

    })
  }

  given TField[Long] with  {
    def dataType: DataType[Long] = SQLDataType.BIGINT.asConvertedDataType(new Converter[java.lang.Long, Long]{

      override def from(databaseObject: java.lang.Long): Long = databaseObject

      override def to(userObject: Long): java.lang.Long = userObject

      override def fromType(): Class[java.lang.Long] = classOf[java.lang.Long]

      override def toType(): Class[Long] = classOf[Long]

    })
  }

  given TField[Float] with {
    def dataType: DataType[Float] = SQLDataType.REAL.asConvertedDataType(new Converter[java.lang.Float, Float]{

      override def from(databaseObject: java.lang.Float): Float = databaseObject

      override def to(userObject: Float): java.lang.Float = userObject

      override def fromType(): Class[java.lang.Float] = classOf[java.lang.Float]

      override def toType(): Class[Float] = classOf[Float]

    })
  }

  given TField[Double] with {
    def dataType: DataType[Double] = SQLDataType.FLOAT.asConvertedDataType(new Converter[java.lang.Double, Double]{

      override def from(databaseObject: java.lang.Double): Double = databaseObject

      override def to(userObject: Double): java.lang.Double = userObject

      override def fromType(): Class[java.lang.Double] = classOf[java.lang.Double]

      override def toType(): Class[Double] = classOf[Double]

    })
  }

  given TField[String] with {
    def dataType: DataType[String] = SQLDataType.VARCHAR.asConvertedDataType(new Converter[java.lang.String, String]{

      override def from(databaseObject: java.lang.String): String = databaseObject

      override def to(userObject: String): java.lang.String = userObject

      override def fromType(): Class[java.lang.String] = classOf[java.lang.String]

      override def toType(): Class[String] = classOf[String]

    })
  }

  given TField[Boolean] with {
    def dataType: DataType[Boolean] = SQLDataType.BOOLEAN.asConvertedDataType(new Converter[java.lang.Boolean, Boolean] {

      override def from(databaseObject: lang.Boolean): Boolean = {
        databaseObject
      }

      override def to(userObject: Boolean): lang.Boolean = {
        userObject
      }

      override def fromType(): Class[lang.Boolean] = classOf[lang.Boolean]

      override def toType(): Class[Boolean] = classOf[Boolean]

    })
  }

  given TField[BigInt] with {
    def dataType: DataType[BigInt] = SQLDataType.DECIMAL_INTEGER(65).asConvertedDataType(new Converter[java.math.BigInteger, BigInt] {

      override def from(databaseObject: BigInteger): BigInt = databaseObject

      override def to(userObject: BigInt): BigInteger = userObject.bigInteger

      override def fromType(): Class[BigInteger] = classOf[BigInteger]

      override def toType(): Class[BigInt] = classOf[BigInt]

    })
  }

  given TField[BigDecimal] with {
    def dataType: DataType[BigDecimal] = SQLDataType.NUMERIC(65,10).asConvertedDataType(new Converter[java.math.BigDecimal, BigDecimal] {

      override def from(databaseObject: java.math.BigDecimal): BigDecimal = {
        databaseObject
      }

      override def to(userObject: BigDecimal): java.math.BigDecimal = {
        userObject.bigDecimal
      }

      override def fromType(): Class[java.math.BigDecimal] = classOf[java.math.BigDecimal]

      override def toType(): Class[BigDecimal] = classOf[BigDecimal]
    })
  }

  given TField[ZonedDateTime] with {
    def dataType: DataType[ZonedDateTime] = SQLDataType.BIGINT.asConvertedDataType(new Converter[java.lang.Long, ZonedDateTime] {

      override def from(databaseObject: lang.Long): ZonedDateTime = {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(databaseObject), ZoneId.systemDefault())
      }

      override def to(userObject: ZonedDateTime): lang.Long = {
        userObject.toInstant().toEpochMilli()
      }

      override def fromType(): Class[lang.Long] = classOf[lang.Long]

      override def toType(): Class[ZonedDateTime] = classOf[ZonedDateTime]

    })
  }

  given TField[ju.Date] with        {
    def dataType: DataType[ju.Date] = SQLDataType.BIGINT.asConvertedDataType(new Converter[java.lang.Long, ju.Date] {

      override def from(databaseObject: lang.Long): ju.Date = {
        ju.Date.from(Instant.ofEpochMilli(databaseObject))
      }

      override def to(userObject: ju.Date): lang.Long = {
        userObject.toInstant().toEpochMilli()
      }

      override def fromType(): Class[lang.Long] = classOf[java.lang.Long]

      override def toType(): Class[ju.Date] = classOf[ju.Date]


    })
  }

  given TField[Array[Byte]] with {
    def dataType: DataType[Array[Byte]] = SQLDataType.BLOB
  }

}
