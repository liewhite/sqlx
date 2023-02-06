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
import java.util.Date
import scala.deriving.Mirror

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
    if (length.isDefined) {
      datatype = datatype.length(length.get)
    }
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

  // given TField[Boolean] with {
  //   def dataType: DataType[_] = SQLDataType.BOOLEAN
  // }

  // given TField[BigInt] with {
  //   def dataType: DataType[_] = SQLDataType.DECIMAL_INTEGER(65)
  // }

  // given TField[BigDecimal] with {
  //   def dataType: DataType[_] = SQLDataType.DECIMAL_INTEGER(65)
  // }

  // given TField[ZonedDateTime] with {
  //   def dataType: DataType[_] = SQLDataType.BIGINT
  // }

  // given TField[Date] with        {
  //   def dataType: DataType[_] = SQLDataType.BIGINT
  // }
  // given TField[Array[Byte]] with {
  //   def dataType: DataType[_] = SQLDataType.BLOB
  // }

}
