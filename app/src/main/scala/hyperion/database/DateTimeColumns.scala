package hyperion.database

import java.time.LocalDate
import java.sql.Date

import slick.jdbc.PostgresProfile.api._

/**
  * Add support for Java 8 Date/Time types in Slick.
  */
trait DateTimeColumns {
  implicit val localDateColumnType = MappedColumnType.base[LocalDate, Date](
    ld => Date.valueOf(ld),
    d => d.toLocalDate
  )
}
