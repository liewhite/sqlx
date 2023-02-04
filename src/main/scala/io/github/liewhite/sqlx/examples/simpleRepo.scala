package io.github.liewhite.sqlx.examples

import io.getquill._
import zio.*
import io.github.liewhite.sqlx.annotation.*
import io.github.liewhite.sqlx.*

object MyApp extends ZIOAppDefault {
  def run = {
    val config = DBConfig("mysql", "localhost", "root", "test")

    (for {
      repo <- ZIO.service[UserRepo]
      xx    <- repo.migrate()
      _ <- Console.printLine(xx)
      _   <- repo.create(User("ojbk"))
      users <- repo.listAll()
      _ <- Console.printLine(users)
    } yield users).provide(
      UserRepoImpl.layer,
      DBDataSource.layer,
      ZLayer.succeed(config)
    )
  }
}
