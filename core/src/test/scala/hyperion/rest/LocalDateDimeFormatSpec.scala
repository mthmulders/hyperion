package hyperion.rest

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import hyperion.BaseSpec
import hyperion.rest.HyperionJsonProtocol.LocalDateDimeFormat
import spray.json.{JsNumber, DeserializationException, JsString}

class LocalDateDimeFormatSpec extends BaseSpec {
  val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  "Converting a LocalDateTime" should {
    "write the value following ISO 8601" in {
      // Arrange
      val str = "2012-04-23T18:25:43.511Z"
      val input = LocalDateTime.parse(str, format)

      // Act
      val result = LocalDateDimeFormat.write(input)

      // Assert
      result.toString() should be(s""""$str"""") // wrapped in double quotes to make it valid JSON
    }

    "parse the value following ISO 8601" in {
      // Arrange
      val input = "2012-04-23T18:25:43.511Z"

      // Act
      val result = LocalDateDimeFormat.read(JsString(input))

      // Assert
      result should be(LocalDateTime.parse(input, format))
    }

    "not parse things that are not in ISO 8601 format" in {
      // Act
      the[DeserializationException] thrownBy {
        LocalDateDimeFormat.read(JsString("bogus"))
        // Assert
      } should have message "Cannot convert bogus to a LocalDateTime"
    }

    "not parse things that are not a string" in {
      // Act
      the[DeserializationException] thrownBy {
        LocalDateDimeFormat.read(JsNumber(3))
        // Assert
      } should have message "Cannot convert 3 to a LocalDateTime"
    }
  }
}
