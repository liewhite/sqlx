package io.github.liewhite.sqlx.examples

import zio.*
import io.github.liewhite.sqlx.annotation.*
// import com.zaxxer.hikari.HikariDataSource
import io.github.liewhite.sqlx.{Table}
import java.sql.SQLException
import scala.util.Try
import io.github.liewhite.sqlx.DBDataSource
import io.github.liewhite.sqlx.DBConfig
import javax.sql.DataSource

@TableName("user1")
case class User(
    @Primary
    id: Long,

    @ColumnName("nick_name")
    name: String)

