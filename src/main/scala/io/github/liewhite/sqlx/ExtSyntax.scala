package io.github.liewhite.sqlx

import io.getquill.*
import io.getquill.context.qzio.ZioJdbcContext
import java.time.ZonedDateTime
import io.getquill.idiom.Idiom
import io.getquill.context.sql.idiom.SqlIdiom
import java.util.Date
import java.time.Instant
import java.time.ZoneId
import io.getquill.parser.engine.Parser
import scala.quoted.Quotes

trait ExtSyntax[I <: SqlIdiom, N <: NamingStrategy] { this: ZioJdbcContext[I, N] =>
    extension [T](inline left: T) {
        inline def >(right: T)  = quote(infix"$left > $right".pure.as[Boolean])
        inline def >=(right: T) = quote(infix"$left >= $right".pure.as[Boolean])
        inline def <(right: T)  = quote(infix"$left < $right".pure.as[Boolean])
        inline def <=(right: T) = quote(infix"$left <= $right".pure.as[Boolean])
        inline def ==(right: T)  = quote(infix"$left = $right".pure.as[Boolean])
    }

    // mysql for update clause
    extension [T](inline q: Query[T]) {
        inline def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
    }
}
