package io.github.liewhite.sqlx.examples

import scala.jdk.CollectionConverters.*
import zio.*
import io.github.liewhite.sqlx.*
import org.jooq.DSLContext
import org.jooq.impl.DefaultDSLContext
import org.jooq.Configuration
import org.jooq.SQLDialect

import io.github.liewhite.sqlx.DBDataSource

import io.github.liewhite.sqlx.Migration
import org.jooq.impl.DefaultDataType
import org.jooq.impl.SQLDataType
import org.jooq.impl.DSL
import org.jooq.Record2

case class OK(
  a: Option[Long],
  b: Detail
)
object MyApp extends ZIOAppDefault {
  def run = {
    val config = DBConfig("mysql", "localhost", "root", "test")

    val q = Table[User]

    (for {
      migResult <- Migration.Migrate[User]
      ctx <- ZIO.service[org.jooq.DSLContext]
      _ <- ZIO.attempt{
        // println(ctx.insertInto(q.table).columns(q.field_age, q.field_detail).values(None,Detail("xxxx")).execute())
        val result = ctx.select(q.field_age,q.field_detail).from(q.table).fetch()
        println(result.as[OK])
        println(result.as[(Option[Long], Detail)])
      }
    } yield ()).provide(
      ZLayer.succeed(config),
      DBDataSource.layer,
      DBContext.layer,
    )
  }
}
