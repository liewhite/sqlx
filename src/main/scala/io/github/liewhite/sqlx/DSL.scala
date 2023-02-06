package io.github.liewhite.sqlx

import scala.compiletime.*
import scala.quoted.*
import scala.deriving.Mirror
import zio.ZIO
import org.jooq.DSLContext
import scala.jdk.CollectionConverters.*
import org.jooq
import org.jooq.InsertValuesStepN

// query 的 selectable 要用refinement type来加入各个table
class Query(val tables: Map[String, Table[_]]) extends Selectable {

  def selectDynamic(name: String): Any = {
    tables(name)
  }
}

object Query {
  // todo 允许声明主键
  def insertOne[T <: Product: Table](
      o: T
    ): ZIO[DSLContext, Nothing, InsertValuesStepN[org.jooq.Record]] = {
    import org.jooq.impl.DSL.*
    val t = summon[Table[T]]
    for {
      ctx <- ZIO.service[DSLContext]
    } yield {
      ctx
        .insertInto(table(t.tableName))
        .columns(t.jooqCols.filter(item => true).asJava)
        .values(t.values(o).asJava)
    }
  }

  def insertMany[T <: Product: Table](
      o: Seq[T]
    ): ZIO[DSLContext, Nothing, InsertValuesStepN[org.jooq.Record]] = {
    import org.jooq.impl.DSL.*
    val t = summon[Table[T]]
    for {
      ctx <- ZIO.service[DSLContext]
    } yield {
      // drop id
      val clause: InsertValuesStepN[org.jooq.Record] =
        ctx.insertInto(table(t.tableName)).columns(t.jooqCols.asJava)
      o.foldLeft(clause)((cls, item) => {
        cls.values(t.values(item).asJava)
      })
    }
  }

  transparent inline def apply[T <: Product: Table] = {
    val t = summon[Table[T]]
    val q = new Query(Map.empty)
    refinement(q, t)
  }

  extension [Q <: Query](q: Q) {
    transparent inline def join[T <: Product](using table: Table[T]) = {
      val newQuery = new Query(q.tables.updated(table.tableName, table))
      refinement(newQuery.asInstanceOf[Q], table)
    }

    inline def where(condition: Q => Condition): AfterWhere[Q] = ???
  }

  transparent inline def refinement[Q <: Query, T <: Product](
      q: Q,
      table: Table[T]
    ) = {
    ${ refinementImpl[Q, T]('q, 'table) }
  }
  def refinementImpl[Q <: Query: Type, T <: Product: Type](
      q: Expr[Q],
      table: Expr[Table[T]]
    )(using Quotes
    ): Expr[Any] = {
    import quotes.reflect.*

    val tableName     = TypeRepr.of[T].classSymbol.get.name
    val tableNameExpr = Expr(tableName)
    Refinement(TypeRepr.of[Q], tableName, TypeRepr.of[Table[T]]).asType match {
      case '[tpe] => {
        val res = '{
          val newQ = new Query(${ q }.tables.updated($tableNameExpr, $table))
          newQ.asInstanceOf[tpe]
        }
        res
      }
    }
  }
}

class Condition {}

class AfterWhere[Q <: Query] {}
