package io.github.liewhite.sqlx.examples

import io.getquill._
import zio.*
import io.github.liewhite.sqlx.annotation.*
import com.zaxxer.hikari.HikariDataSource
import io.github.liewhite.sqlx.{Table, QuillMysqlContext}
import java.sql.SQLException
import scala.util.Try

@TableName("ttt")
case class T(
    @ColumnName("nnnn")
    name: String)

trait Repo {
  def migrate(): ZIO[HikariDataSource, Nothing, Try[Unit]]
  def create(t: T): ZIO[HikariDataSource, SQLException, Long]
}

class RepoImpl(datasource: HikariDataSource) extends Repo {

  val table = Table[T]
  val ctx   = QuillMysqlContext(table.ns)
  import ctx.*

  override def migrate(): ZIO[HikariDataSource, Nothing, Try[Unit]] = {
    ctx.migrate[T]
  }

  override def create(t: T) = {
    for {
      result <- run(query[T].insertValue(lift(t)))
    } yield result
  }
}

object RepoImpl {
  def layer: ZLayer[HikariDataSource, Nothing, RepoImpl] = {
    ZLayer.fromZIO(
      for {
        ds <- ZIO.service[HikariDataSource]
      } yield new RepoImpl(ds)
    )
  }
}

object MyApp extends ZIOAppDefault {
  def run = {
    val datasource = new HikariDataSource()
    datasource.setJdbcUrl(s"jdbc:mysql://localhost:3306/test")
    datasource.setUsername("root")
    // datasource.setMaximumPoolSize(config.maxConnection)
    // datasource.setMinimumIdle(config.minIdle)
    // datasource.setIdleTimeout(config.idleMills)

    // if (config.password.isDefined) {
    //   datasource.setPassword(config.password.get)
    // }

    (for {
      repo <- ZIO.service[Repo]
      _    <- repo.migrate()
      id   <- repo.create(T("ojbk"))
      _    <- Console.printLine(id)
    } yield id).provide(
      RepoImpl.layer,
      ZLayer.succeed(datasource)
    )
  }
}
