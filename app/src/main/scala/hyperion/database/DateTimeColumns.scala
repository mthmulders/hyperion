package hyperion.database

import java.time.LocalDate
import java.sql.Date

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

/**
  * Add support for Java 8 Date/Time types in Slick.
  */
trait DateTimeColumns {
  implicit val localDateColumnType: JdbcType[LocalDate] with BaseTypedType[LocalDate] = MappedColumnType.base[LocalDate, Date](
    ld => Date.valueOf(ld),
    d => d.toLocalDate
  )
}
