package io.github.liewhite.sqlx

import io.getquill.*
import io.getquill.context.jdbc.JdbcContext
import java.time.ZonedDateTime
import io.getquill.idiom.Idiom
import io.getquill.context.sql.idiom.SqlIdiom
import java.util.Date
import java.time.Instant
import java.time.ZoneId
import io.getquill.parser.engine.Parser
import scala.quoted.Quotes

trait ExtSyntax[I <: SqlIdiom, N <: NamingStrategy] { this: JdbcContext[I, N] =>
    // extension [T](inline left: T) {
    //     inline def gt(right: T)  = quote(infix"$left > $right".pure.as[Boolean])
    //     inline def gte(right: T) = quote(infix"$left >= $right".pure.as[Boolean])
    //     inline def lt(right: T)  = quote(infix"$left < $right".pure.as[Boolean])
    //     inline def lte(right: T) = quote(infix"$left <= $right".pure.as[Boolean])
    //     inline def eq(right: T)  = quote(infix"$left = $right".pure.as[Boolean])
    // }

    // mysql for update clause
    extension [T](inline q: Query[T]) {
        inline def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
    }
}
