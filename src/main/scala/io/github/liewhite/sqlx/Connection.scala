package io.github.liewhite.sqlx

import scala.jdk.CollectionConverters.*
import org.jooq.{SQLDialect, DSLContext}
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.jooq
import java.sql.DriverManager
import scala.collection.mutable
import org.jooq.impl.SQLDataType
import com.zaxxer.hikari.HikariDataSource
import io.getquill.*
import io.getquill.context.sql.idiom.SqlIdiom
import scala.compiletime.erasedValue
import io.getquill.context.jdbc.JdbcContext
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.StrictLogging
import org.jooq.DataType
import java.time.ZonedDateTime
import java.time.Instant
import java.time.ZoneId

case class DBConfig(
    host: String,
    username: String,
    db: String,
    port: Option[Int] = None,
    password: Option[String] = None,
    maxConnection: Int = 20,
    minIdle: Int = 1,
    idleMills: Int = 60 * 1000)

class QuillMysqlContext(dataSource: HikariDataSource)
    extends MysqlJdbcContext(Literal, dataSource)
    with ExtSyntax[MySQLDialect, Literal.type]
    // migrator可以针对不同driver提供， 现在先用同一份
    with Migrator[MySQLDialect, Literal.type] {
  // 只能在子类中覆盖codec， 在其他 trait中覆盖会歧义
  implicit val bigIntDecoder: Decoder[BigInt] =
    decoder(row =>
      index => {
        BigInt(row.getObject(index).toString)
      }
    )

  implicit val bigIntEncoder: Encoder[BigInt]       = {
    encoder(
      java.sql.Types.NUMERIC,
      (index, value, row) =>
        row.setObject(
          index,
          java.math.BigDecimal(value.toString),
          java.sql.Types.NUMERIC
        )
    )
  }
  implicit val zonedDecoder: Decoder[ZonedDateTime] =
    decoder(row =>
      index => {
        ZonedDateTime.ofInstant(
          Instant.ofEpochMilli(row.getLong(index)),
          ZoneId.systemDefault
        )
      }
    )

  implicit val zonedEncoder: Encoder[ZonedDateTime] = {
    encoder(
      java.sql.Types.BIGINT,
      (index, value, row) =>
        row.setObject(
          index,
          value.toInstant.toEpochMilli,
          java.sql.Types.BIGINT
        )
    )
  }
}

class QuillPostgresContext(dataSource: HikariDataSource)
    extends PostgresJdbcContext(Literal, dataSource)
    with ExtSyntax[PostgresDialect, Literal.type]
    with Migrator[PostgresDialect, Literal.type] {

  override implicit val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    decoder(row =>
      index => {
        ZonedDateTime.ofInstant(
          Instant.ofEpochMilli(row.getLong(index)),
          ZoneId.systemDefault
        )
      }
    )

  override implicit val zonedDateTimeEncoder: Encoder[ZonedDateTime] = {
    encoder(
      java.sql.Types.BIGINT,
      (index, value, row) =>
        row.setObject(
          index,
          value.toInstant.toEpochMilli,
          java.sql.Types.BIGINT
        )
    )
  }
}

transparent inline def getDBContext[Dialect <: SqlIdiom](config: DBConfig) = {
  val prefix = inline erasedValue[Dialect] match {
    case MySQLDialect    => "jdbc:mysql://"
    case PostgresDialect => "jdbc:postgresql://"
  }

  val defaultPort = inline erasedValue[Dialect] match {
    case MySQLDialect    => 3306
    case PostgresDialect => 5432
  }
  val port        = config.port match {
    case Some(p) => p
    case None    => defaultPort
  }

  val datasource = new HikariDataSource()
  datasource.setJdbcUrl(s"${prefix}${config.host}:${port}/${config.db}")
  datasource.setUsername(config.username)
  datasource.setMaximumPoolSize(config.maxConnection)
  datasource.setMinimumIdle(config.minIdle)
  datasource.setIdleTimeout(config.idleMills)

  if (config.password.isDefined) {
    datasource.setPassword(config.password.get)
  }
  inline erasedValue[Dialect] match {
    case MySQLDialect    => QuillMysqlContext(datasource)
    case PostgresDialect => QuillPostgresContext(datasource)
  }
}

trait Migrator[Dialect <: SqlIdiom, Naming <: NamingStrategy] {
  this: JdbcContext[Dialect, Naming] =>
  val migratorLogger: Logger        = Logger("migration")
  def migrate[T](using t: Table[T]) = {
    val connection = dataSource.getConnection
    try {
      doMigrate[T](connection)
    } finally {
      connection.close
    }
  }

  enum DBDriver {
    case MySQL
    case PostgreSQL
    case Others
  }

  private def doMigrate[T](jdbc: java.sql.Connection)(using table: Table[T]) = {
    val driverName = jdbc.getMetaData.getDriverName
    val driver     = if (driverName.contains("PostgreSQL")) {
      DBDriver.PostgreSQL
    } else if (driverName.contains("MySQL")) {
      DBDriver.MySQL
    } else {
      DBDriver.Others
    }

    // "PostgreSQL JDBC Driver"
    // "MySQL Connector/J"
    val jooqConn                              = DSL.using(jdbc)
    var metaCache: jooq.Meta                  = jooqConn.meta()
    val tables: mutable.Map[String, Table[_]] = mutable.Map.empty

    val tableName = table.tableName
    getTable(tableName) match {
      case None    => createTable(table)
      case Some(t) => updateTable(table, t)
    }

    def getTable(name: String): Option[jooq.Table[_]] = {
      val result = metaCache.getTables(name)
      if (result.isEmpty) {
        None
      } else {
        Some(result.get(0))
      }
    }
    def createTable(table: Table[_])                  = {
      if (table.columnsMap.contains("id")) {
        throw Exception("custom id not allowed on :" + table.tableName)
      }
      // default and nullable
      val createStmt = {
        val create       = jooqConn.createTable(table.tableName)
        val createWithID =
          create.column("id", SQLDataType.BIGINT.identity(true))
        table.columns.foldLeft(createWithID)((b, col) => {
          var datatype = col.getDataType
          var c        = b.column(col.colName, datatype)
          c
        })
      }
      createStmt.primaryKey("id").execute

      // add unique constraint
      table.columns.foreach(item => {
        if (item.unique) {
          jooqConn
            .alterTable(table.tableName)
            .add(constraint(item.uniqueKeyName).unique(item.colName))
            .execute
        }
      })

      table.indexes.foreach(idx => {
        if (idx.unique) {
          jooqConn
            .createUniqueIndex(idx.indexName)
            .on(table.tableName, idx.cols*)
            .execute
        } else {
          jooqConn
            .createIndex(idx.indexName)
            .on(table.tableName, idx.cols*)
            .execute
        }
      })
    }

    def updateTable(
        table: Table[_],
        current: jooq.Table[_]
      ) = {
      // 新增column, column 比较， 只新增， 不删除, 不重命名
      table.columns.foreach(col => {
        if (current.field(col.colName) == null) {
          createColumn(current, col)
        } else {
          val datatype = col.getDataType
          jooqConn.alterTable(current).alter(col.colName).set(datatype).execute
          if (col.default.isDefined) {
            jooqConn
              .alterTable(current)
              .alter(col.colName)
              .setDefault(col.default.get)
              .execute
          } else {
            if (col.t.nullable) {
              jooqConn
                .alterTable(current)
                .alter(col.colName)
                .dropDefault()
                .execute
            } else {
              migratorLogger.info(
                s"skip dropping default on not null column: ${col.modelName}.${col.colName}"
              )
            }
          }
        }
      })
      // postgresql: 定义为Unique的会出现在这里 : "uk_xx"
      // mysql: 定义为Unique或者唯一索引会出现在这里, 如果同时定义了unique和唯一索引，会出现多次
      // 用库中结构来适配代码定义
      val currentUniques = current.getUniqueKeys.asScala
        .map(_.getName)
        .toSet
      val defineUniques  =
        table.columns.filter(_.unique).map(_.uniqueKeyName).toSet

      (defineUniques -- currentUniques).foreach(item => {
        jooqConn
          .alterTable(table.tableName)
          .add(constraint(item).unique(item.stripPrefix("uk:")))
          .execute
      })
      (currentUniques -- defineUniques).foreach(item => {
        if (item.startsWith("uk:")) {
          jooqConn
            .alterTable(table.tableName)
            .drop(constraint(item).unique(item.stripPrefix("uk:")))
            .execute
        }
      })

      // Mysql:  会查询到所有普通索引, 没有唯一索引和唯一约束
      // postgres: 查询到所有索引， 没有唯一约束
      val oldIdxes =
        (current.getIndexes.asScala.map(item => item.getName) ++ currentUniques
          .filter(
            _.startsWith("ui:")
          )).filter(!_.startsWith("uk:"))

      val newIdxes = table.indexes.map(item => item.indexName)

      newIdxes.foreach(idx => {
        if (!oldIdxes.contains(idx)) {
          val names_unique = idx.split(":")
          val names        = names_unique(1).split("-").toVector
          val unique       = if (names_unique(0) == "i") false else true
          if (unique) {
            jooqConn
              .createUniqueIndex(idx)
              .on(current.getName, names*)
              .execute
          } else {
            jooqConn
              .createIndex(idx)
              .on(current.getName, names*)
              .execute
          }
        }
      })

      oldIdxes.foreach(idx => {
        val names_unique = idx.split(":")
        val names        = names_unique(1).split("-").toVector
        val unique       = if (names_unique(0) == "i") false else true
        if (!newIdxes.contains(idx)) {
          jooqConn
            .dropIndex(idx)
            .on(current.getName)
            .execute
        }
      })
    }

    def createColumn(
        jooqTable: jooq.Table[_],
        col: Field[_]
      ) = {
      jooqConn
        .alterTable(jooqTable)
        .addColumn(col.colName, col.getDataType)
        .execute
    }
  }
}
