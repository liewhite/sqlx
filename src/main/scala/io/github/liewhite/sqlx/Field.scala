package io.github.liewhite.sqlx

import java.sql.ResultSet
import java.sql.PreparedStatement
import java.sql.SQLException
import org.jooq.DataType
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
  def dataType: DataType[_]
}

object TField {
  given [T](using t: TField[T]): TField[Option[T]] with {
    override def nullable: Boolean = true
    def dataType: DataType[_]      = t.dataType.nullable(true)
  }

  given TField[Int] with {
    def dataType: DataType[_] = SQLDataType.INTEGER
  }

  given TField[Long] with  {
    def dataType: DataType[_] = SQLDataType.BIGINT
  }
  given TField[Float] with {
    def dataType: DataType[_] = SQLDataType.FLOAT
  }

  given TField[Double] with {
    def dataType: DataType[_] = SQLDataType.DOUBLE
  }

  given TField[String] with {
    def dataType: DataType[_] = SQLDataType.VARCHAR(255)
  }

  given TField[Boolean] with {
    def dataType: DataType[_] = SQLDataType.BOOLEAN
  }

  given TField[BigInt] with {
    def dataType: DataType[_] = SQLDataType.DECIMAL_INTEGER(65)
  }

  given TField[BigDecimal] with {
    def dataType: DataType[_] = SQLDataType.DECIMAL_INTEGER(65)
  }

  given TField[ZonedDateTime] with {
    def dataType: DataType[_] = SQLDataType.BIGINT
  }

  given TField[Date] with        {
    def dataType: DataType[_] = SQLDataType.BIGINT
  }
  given TField[Array[Byte]] with {
    def dataType: DataType[_] = SQLDataType.BLOB
  }
}
