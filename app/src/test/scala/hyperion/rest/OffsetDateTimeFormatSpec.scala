package hyperion.rest

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import hyperion.BaseSpec
import hyperion.rest.HyperionJsonProtocol.OffsetDateTimeFormat
import spray.json.{JsNumber, DeserializationException, JsString}

class OffsetDateTimeFormatSpec extends BaseSpec {
  val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  "Converting a LocalDateTime" should {
    "write the value following ISO 8601 converted to local time zone" in {
      // Arrange
      val str = "2012-04-23T18:25:43.511Z"
      val input = OffsetDateTime.parse(str, format)

      // Act
      val result = OffsetDateTimeFormat.write(input)

      // Assert
      result.toString() should be(s""""$str"""") // wrapped in double quotes to make it valid JSON
    }

    "parse the value following ISO 8601" in {
      // Arrange
      val input = "2012-04-23T18:25:43.511Z"

      // Act
      val result = OffsetDateTimeFormat.read(JsString(input))

      // Assert
      result should be(OffsetDateTime.parse(input, format))
    }

    "not parse things that are not in ISO 8601 format" in {
      // Act
      the[DeserializationException] thrownBy {
        OffsetDateTimeFormat.read(JsString("bogus"))
        // Assert
      } should have message "Cannot convert bogus to a ZonedDateTime"
    }

    "not parse things that are not a string" in {
      // Act
      the[DeserializationException] thrownBy {
        OffsetDateTimeFormat.read(JsNumber(3))
        // Assert
      } should have message "Cannot convert 3 to a ZonedDateTime"
    }
  }
}
