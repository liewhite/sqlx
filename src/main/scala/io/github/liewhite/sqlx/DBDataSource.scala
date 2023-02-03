package io.github.liewhite.sqlx

import com.zaxxer.hikari.HikariDataSource
import zio.ZIO
import zio.ZLayer

class DBDataSource(val config: DBConfig) {
  val datasource = new HikariDataSource()
  val port       = config.port.getOrElse(
    if (config.`type` == "mysql") {
      3306
    } else if (config.`type` == "postgresql") {
      5432
    } else {
      throw Exception("not support db:" + config.`type`)
    }
  )
  datasource.setJdbcUrl(s"jdbc:${config.`type`}://${config.host}:${port}/${config.db}")
  datasource.setUsername(config.username)
  datasource.setMaximumPoolSize(config.maxConnection)
  datasource.setMinimumIdle(config.minIdle)
  datasource.setIdleTimeout(config.idleMills)
  if (config.password.isDefined) {
    datasource.setPassword(config.password.get)
  }
}

object DBDataSource {
  def layer: ZLayer[DBConfig, Nothing, DBDataSource] = {
    ZLayer {
      for {
        config <- ZIO.service[DBConfig]
      } yield DBDataSource(config)
    }
  }
}
