import io.github.liewhite.sqlx.*
import io.github.liewhite.sqlx.annotation.{Unique, Index, Length}
import io.getquill.*
import java.time.ZonedDateTime
import org.jooq.impl.SQLDataType
import org.jooq.DataType
import java.time.Instant
import java.time.ZoneId

class CustomField(val value: String)

// custom datatypes support with just serveral givens
object CustomField {
    given TField[CustomField] with {
        def dataType: DataType[_] = SQLDataType.CLOB
    }
    given MappedEncoding[CustomField, String](_.value)
    given MappedEncoding[String, CustomField](CustomField(_))
}

case class TTT(
    @Unique // create unique constraint on table
    fId: Long,

    @Index("a-b", unique = true)
    i:   BigInt,

    @Length(35)   // column length, works for type with length(like varchar), or ignore
    @Index("a-b", unique = true) // index with same name will create multi-column index
    s: String = "default in db", // this'll set default value in table

    dt: ZonedDateTime,

    @Length(35)
    os: Option[String], // nullable in table

    @Length(1000)
    customField: CustomField // use custom datatypes
)

@main def main: Unit = {
    // connnect to db
    val ctx = getDBContext[MySQLDialect.type](
      DBConfig(
        host = "localhost",
        username = "sa",
        password = Some("123"),
        db = "test"
      )
    )
    import ctx._

    // auto mapping case class to db table
    ctx.migrate[TTT]

    // insert into table
    run(query[TTT].insertValue(lift(TTT(2, 2,  "Bob", ZonedDateTime.now(), None, CustomField("Alice")))))

    // query from table
    // val rows = run(query[T].filter(item => liftQuery(Vector(1,2,3)).contains( item.i)).forUpdate)
    val rows = run(query[TTT].filter(item => item.i == lift(BigInt(1))).forUpdate)

    rows.foreach(println)
}
