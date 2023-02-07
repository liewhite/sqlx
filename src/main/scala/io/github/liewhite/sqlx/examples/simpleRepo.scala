package io.github.liewhite.sqlx.examples

import zio.*
import io.github.liewhite.sqlx.annotation.*
import io.github.liewhite.sqlx.*
import org.jooq.DSLContext
import org.jooq.impl.DefaultDSLContext
import org.jooq.Configuration
import org.jooq.SQLDialect

import io.github.liewhite.sqlx.DBDataSource

import io.github.liewhite.sqlx.Migration

object MyApp extends ZIOAppDefault {
  def run = {
    val config = DBConfig("mysql", "localhost", "root", "test")

    val q = Table[User]

    // val fields = q.fields
    (for {
      migResult <- Migration.Migrate[User]
      ctx <- ZIO.service[org.jooq.DSLContext]
      _ <- ZIO.attempt{
        ctx.insertInto(q.table).columns(q.field_name,q.field_p, q.field_detail).values("xxx", BigDecimal("11111.222222"), Detail("xxxx")).execute()
        ctx.select(q.field_name, q.field_detail).from(q.table).where(q.field_name.eq("xxx")).fetch()
      }
      result <- ZIO.attempt{
        ctx.select(q.field_name, q.field_detail).from(q.table).where(q.field_name.eq("xxx")).fetch()
      }
      _ <- Console.printLine(result)
    } yield ()).provide(
      ZLayer.succeed(config),
      DBDataSource.layer,
      DBContext.layer,
    )
  }
}
