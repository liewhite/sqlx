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
    val user= User(0, "leeliewhite")
    val q = Query[User].User
    println(q.name)

    (for {
      migResult <- Migration.Migrate[User]
      q1 <- Query.insertOne(user)
      q2 <- Query.insertMany(Vector(user,user,user))
      _ <- Console.printLine(q1.execute(), q2.execute())
    } yield migResult).provide(
      ZLayer.succeed(config),
      DBDataSource.layer,
      DBContext.layer,
    )
  }
}
