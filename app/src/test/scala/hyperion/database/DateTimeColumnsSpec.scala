package hyperion.database

import java.sql.{Date, PreparedStatement, ResultSet}
import java.time.LocalDate

import hyperion.BaseSpec
import org.scalatest.OneInstancePerTest
import org.scalamock.scalatest.proxy.MockFactory

class DateTimeColumnsSpec extends BaseSpec with DateTimeColumns with OneInstancePerTest with MockFactory {
  /*
  "The DateTimeColumns" should {
    "convert a java.time.LocalDate to a java.sql.Date" in {
      // Arrange
      val now = LocalDate.now()
      val preparedStatement = stub[PreparedStatement]

      // Act
      localDateColumnType.setValue(now, preparedStatement, 1)

      // Assert
      (preparedStatement.setDate _).verify(1, Date.valueOf(now))
    }

    "convert a java.sql.Date to a java.time.LocalDate" in {
      // Arrange
      val unixEpoch = new Date(0)
      val resultSet = stub[ResultSet]
      (resultSet.getDate _).expects(1, null).returning(unixEpoch)

      // Act
      val result = localDateColumnType.getValue(resultSet, 1)

      // Assert
      result shouldBe unixEpoch.toLocalDate
    }
  }
  */

  /*
  It seems Scala doesn't understand we don't call any Java API (like PreparedStatement#getDate or
  PreparedStatement#setDate) with tuples. That's why the compiler gives:
    [error] hyperion/app/src/test/scala/hyperion/database/DateTimeColumnsSpec.scala:21: ambiguous reference to overloaded definition,
    [error] both method setDate in trait PreparedStatement of type (x$1: Int, x$2: java.sql.Date, x$3: java.util.Calendar)Unit
    [error] and  method setDate in trait PreparedStatement of type (x$1: Int, x$2: java.sql.Date)Unit
  and
    [error] hyperion/app/src/test/scala/hyperion/database/DateTimeColumnsSpec.scala:28: ambiguous reference to overloaded definition,
    [error] both method getDate in trait ResultSet of type (x$1: String, x$2: java.util.Calendar)java.sql.Date
    [error] and  method getDate in trait ResultSet of type (x$1: Int, x$2: java.util.Calendar)java.sql.Date

  It doesn't look like this is going to be fixed: https://issues.scala-lang.org/browse/SI-2991.
  As for now, disable these tests completely.
   */
}
