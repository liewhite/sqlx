package io.github.liewhite.sqlx

import scala.compiletime.*
import scala.quoted.*
import scala.deriving.Mirror

import shapeless3.deriving.{K0, Continue, Labelling}
import io.github.liewhite.common.SummonUtils.summonAll
import io.github.liewhite.common.{RepeatableAnnotation, RepeatableAnnotations}
import io.github.liewhite.sqlx.annotation
import io.github.liewhite.common.DefaultValue
import org.jooq
import io.getquill.NamingStrategy
import io.getquill.MysqlEscape
import io.getquill.CompositeNamingStrategy2
import io.getquill.PostgresEscape

class DriverNotSupportError(driver: String)
    extends Exception(s"driver not support: $driver")

case class Index(
    name: String,
    cols: Vector[String],
    unique: Boolean) {
  def indexName: String = {
    val prefix = if (unique) "ui:" else "i:"
    prefix + cols.mkString("-")
  }
}

trait CustomNs extends NamingStrategy
// migrate时， 先拿到meta，
// 然后将diff apply 到db
// 再从database meta 恢复出table,用作后续jooq的操作
trait Table[T] {
  def driver: String
  def tableName: String
  def indexes: Vector[Index]
  // def colsMap: Map[String, ]
  def columns: Vector[Field[_]]
  // def namingStrategy(): NamingStrategy = {
  def nameMap = columns.map(item => (item.fieldName, item.colName)).toMap
  //   new TableNamingStrategy((s:String) => tableName, (s:String)=> nameMap(s))
  // }

  def ns: CompositeNamingStrategy2[CustomNs, NamingStrategy] = {
    val renameNs = new CustomNs {
      override def table(s: String): String  = {
        tableName
      }
      override def column(s: String): String = nameMap(s)
      def default(s: String): String         = s
    }
    CompositeNamingStrategy2(
      renameNs,
      if (driver == "mysql") { MysqlEscape }
      else if (driver == "postgresql") { PostgresEscape }
      else {
        throw DriverNotSupportError(driver)
      }
    )
  }
}

object Table {
  def apply[T: Table] = summon[Table[T]]

  inline given derived[A](using
      gen: Mirror.ProductOf[A],
      labelling: Labelling[A],
      index: RepeatableAnnotations[annotation.Index, A],
      unique: RepeatableAnnotations[annotation.Unique, A],
      length: RepeatableAnnotations[annotation.Length, A],
      defaultValue: DefaultValue[A],
      renamesAnn: RepeatableAnnotations[annotation.ColumnName, A],
      tableNameAnn: RepeatableAnnotation[annotation.TableName, A],
      driverAnn: RepeatableAnnotation[annotation.Driver, A]
  ): Table[A] = {
    val defaults        = defaultValue.defaults
    val columnTypes     = summonAll[TField, gen.MirroredElemTypes]
    val customTableName = tableNameAnn()
    val customDriverAnn = driverAnn()
    val customDriver    = if (customDriverAnn.isEmpty) {
      "mysql"
    } else {
      customDriverAnn.head.name
    }

    val tName           = if (customTableName.isEmpty) {
      labelling.label
    } else {
      customTableName.head.name
    }

    // val tName           = labelling.label
    // scala name
    val scalaFieldNames = labelling.elemLabels.toVector
    // db name
    val renames         = renamesAnn()
      .map(col => {
        if (col.isEmpty) {
          None
        } else {
          Some(col.head.name)
        }
      })
      .toVector

    val dbColNames = renames.zip(scalaFieldNames).map {
      case (rename, oriName) => {
        rename.getOrElse(oriName)
      }
    }
    // 仍然保存原始scala name
    // val dbColNames = scalaFieldNames

    val uniques = unique().map(item => if (item.isEmpty) false else true)

    val len = length().map(item => if (item.isEmpty) None else Some(item(0).l))

    val idxes = index().zipWithIndex
      .filter(!_._1.isEmpty)
      .map(item => {
        item._1.map(i =>
          (
            i.copy(priority = if (i.priority != 0) i.priority else item._2),
            dbColNames(item._2)
          )
        )
      })
      .flatten

    val groupedIdx = idxes
      .groupBy(item => item._1.name)
      .map {
        case (name, items) => {
          Index(name, items.map(_._2).toVector, items(0)._1.unique)
        }
      }
      .toVector

    val cols = scalaFieldNames.zipWithIndex.map {
      case (name, index) => {
        val tp      = columnTypes(index)
        val unique  = uniques(index)
        val default = defaults.get(name)
        val typeLen = len(index)

        Field(tName, name, dbColNames(index), unique, default, typeLen, tp)
      }
    }

    new Table {

      def driver: String = customDriver

      def tableName = tName
      def indexes   = groupedIdx
      def columns   = cols.toVector
    }
  }
}
