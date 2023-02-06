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

case class Detail(email:String) derives JsonEncoder, JsonDecoder

object Detail {
    given TField[Detail] with {
      override def dataType: DataType[Detail] = SQLDataType.CLOB.asConvertedDataType(new Converter[String,Detail]{

        override def from(databaseObject: String): Detail = {
            databaseObject.fromJson[Detail].toOption.get
        }

        override def to(userObject: Detail): String = userObject.toJson

        override def fromType(): Class[String] = classOf[String]

        override def toType(): Class[Detail] = classOf[Detail]

      } )
    }
}
@TableName("user1")
case class User(
    @Primary
    id: Long,

    detail: Detail,

    @ColumnName("nick_name")
    name: String)

