package hyperion.rest

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import hyperion.BaseSpec
import hyperion.rest.HyperionJsonProtocol.LocalDateFormat
import spray.json.{DeserializationException, JsNumber, JsString}

class LocalDateFormatSpec extends BaseSpec {
  val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  "Converting a LocalDateFormat" should {
    "write the value following ISO 8601" in {
      // Arrange
      val str = "2012-04-23"
      val input = LocalDate.parse(str, format)

      // Act
      val result = LocalDateFormat.write(input)

      // Assert
      result.toString() should be(s""""$str"""") // wrapped in double quotes to make it valid JSON
    }

    "parse the value following ISO 8601" in {
      // Arrange
      val input = "2012-04-23"

      // Act
      val result = LocalDateFormat.read(JsString(input))

      // Assert
      result should be(LocalDate.parse(input, format))
    }

    "not parse things that are not in ISO 8601 format" in {
      // Act
      the[DeserializationException] thrownBy {
        LocalDateFormat.read(JsString("bogus"))
        // Assert
      } should have message "Cannot convert bogus to a LocalDate"
    }

    "not parse things that are not a string" in {
      // Act
      the[DeserializationException] thrownBy {
        LocalDateFormat.read(JsNumber(3))
        // Assert
      } should have message "Cannot convert 3 to a LocalDate"
    }
  }
}
