package io.github.liewhite.sqlx

import zio.ZIO
import com.zaxxer.hikari.HikariDataSource
import zio.Task

/**
  * Model ctx
  *
  * @param datasource
  * @param table
  */
class TableCtx(datasource: HikariDataSource, table: Table[_]) {}

object TableCtx {
  def apply[T:Table]() = {
    for {
      datasource <- ZIO.service[HikariDataSource]
    } yield new TableCtx(datasource, summon[Table[T]])
  }
}
