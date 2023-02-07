package io.github.liewhite.sqlx.examples

import scala.jdk.CollectionConverters.*
import zio.*
import io.github.liewhite.sqlx.annotation.*
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

object MyApp extends ZIOAppDefault {
  def run = {
    val config = DBConfig("mysql", "localhost", "root", "test")

    val q = Table[User]
    val sss = DSL.`val`(Some(123), summon[TField[Option[Int]]].dataType)

    (for {
      migResult <- Migration.Migrate[User]
      ctx <- ZIO.service[org.jooq.DSLContext]
      _ <- ZIO.attempt{
        println(ctx.insertInto(q.table).columns(q.field_name, q.field_detail).values("xxx",Detail("xxxx")).execute())
      }
    } yield ()).provide(
      ZLayer.succeed(config),
      DBDataSource.layer,
      DBContext.layer,
    )
  }
}
