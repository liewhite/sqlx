import io.getquill._
import zio.*
import io.github.liewhite.sqlx.annotation.*
import com.zaxxer.hikari.HikariDataSource
import io.github.liewhite.sqlx.Table
import io.github.liewhite.sqlx.QuillMysqlContext
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

@TableName("ttt")
case class T(name: String)

object MyApp extends ZIOAppDefault {
  def run = {
    println("xxxxxxxxxxxxxx")

    val datasource = new HikariDataSource()
    datasource.setJdbcUrl(s"jdbc:mysql://localhost:3306/test")
    datasource.setUsername("root")
    // datasource.setMaximumPoolSize(config.maxConnection)
    // datasource.setMinimumIdle(config.minIdle)
    // datasource.setIdleTimeout(config.idleMills)

    // if (config.password.isDefined) {
    //   datasource.setPassword(config.password.get)
    // }

    val table = Table[T]
    val ctx = QuillMysqlContext(table.ns)

    import ctx._
    (for {
      _ <- Console.printLine("start..............")
      e <- ctx.migrate[T]
      result <- ctx.run(query[T].insertValue(ctx.lift(T("lilin"))))
      _ <- Console.printLine(result)
    } yield result).provide(
      ZLayer.succeed(datasource)
    )
  }
}
