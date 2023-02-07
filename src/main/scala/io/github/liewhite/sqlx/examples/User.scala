package io.github.liewhite.sqlx.examples

import zio.*
import zio.json.*
import io.github.liewhite.sqlx.annotation.*
// import com.zaxxer.hikari.HikariDataSource
import io.github.liewhite.sqlx.{Table}
import java.sql.SQLException
import scala.util.Try
import io.github.liewhite.sqlx.DBDataSource
import io.github.liewhite.sqlx.DBConfig
import javax.sql.DataSource
import io.github.liewhite.sqlx.TField
import org.jooq.DataType
import org.jooq.impl.SQLDataType
import org.jooq.Converter
import org.jooq.util.mysql.MySQLDataType

case class Detail(email: String) derives JsonEncoder, JsonDecoder

object Detail {
  given TField[Detail] with {
    def innerDataType: DataType[Detail] =
      SQLDataType.VARCHAR.asConvertedDataType(new Converter[String, Detail] {

        override def from(databaseObject: String): Detail = {
          databaseObject.fromJson[Detail].toOption.get
        }

        override def to(userObject: Detail): String = userObject.toJson

        override def fromType(): Class[String] = classOf[String]

        override def toType(): Class[Detail] = classOf[Detail]

      })
  }
}
@TableName("user1")
case class User(
    @Primary
    id: Long,

    age: Option[String],
    age2: Option[String],
    age3: Option[String],
    age4: Option[String],
    age5: String,
    age6: String,
    age7: String,
    age8: String,
    age9: String,
    age10: String,
    xxx: Option[Int],

    @Precision(65, 10)
    p: BigDecimal,

    @ColumnName("nick_name")
    @Length(100)
    name: String,

    @ColumnName("details")
    @Length(100)
    detail: Detail)
