package io.github.liewhite.sqlx.examples

import io.getquill._
import zio.*
import io.github.liewhite.sqlx.annotation.*
// import com.zaxxer.hikari.HikariDataSource
import io.github.liewhite.sqlx.{Table, QuillMysqlContext}
import java.sql.SQLException
import scala.util.Try
import io.github.liewhite.sqlx.DBDataSource
import io.github.liewhite.sqlx.DBConfig
import javax.sql.DataSource

@TableName("liewhite_user")
case class User(
    @ColumnName("nick_name")
    name: String)

trait UserRepo {
  def migrate(): ZIO[Any, Throwable, Unit]
  def create(t: User): ZIO[Any, SQLException, Long]
  def listAll(): ZIO[Any, SQLException, Vector[User]]
}

class UserRepoImpl(datasource: DBDataSource) extends UserRepo {

  val table = Table[User]
  val ctx   = QuillMysqlContext(table.ns)
  import ctx.*

  override def migrate(): ZIO[Any, Throwable, Unit] = {
    ctx.migrate[User].provide(ZLayer.succeed(datasource))
  }

  override def create(t: User) = {
    run(query[User].insertValue(lift(t))).provide(ZLayer.succeed(datasource.datasource))
  }
  override def listAll() = {
    run(query[User]).provide(ZLayer.succeed(datasource.datasource)).map(_.toVector)
  }
}

object UserRepoImpl {
  def layer: ZLayer[DBDataSource, Nothing, UserRepoImpl] = {
    ZLayer.fromZIO(
      for {
        ds <- ZIO.service[DBDataSource]
      } yield new UserRepoImpl(ds)
    )
  }
}

