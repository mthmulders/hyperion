package hyperion.database

import java.sql.{Date, PreparedStatement, ResultSet}
import java.time.LocalDate

import hyperion.BaseSpec
import org.scalamock.scalatest.MockFactory

class DateTimeColumnsSpec extends BaseSpec with DateTimeColumns with MockFactory {
  "The DateTimeColumns" should {
    "convert a java.time.LocalDate to a java.sql.Date" in {
      // Arrange
      val now = LocalDate.now()
      val preparedStatement = stub[PreparedStatement]

      // Act
      localDateColumnType.setValue(now, preparedStatement, 1)

      // Assert
      (preparedStatement.setDate(_: Int, _: Date)) verify (1, Date.valueOf(now))
    }

    "convert a java.sql.Date to a java.time.LocalDate" in {
      // Arrange
      val unixEpoch = new Date(0)
      val resultSet = stub[ResultSet]
      (resultSet.getDate(_: Int)) when 1 returns unixEpoch

      // Act
      val result = localDateColumnType.getValue(resultSet, 1)

      // Assert
      result shouldBe unixEpoch.toLocalDate
    }
  }
}
