package hyperion

class RingBufferSpec extends BaseSpec {
  "A RingBuffer" should {
    "not contain more elements than its limit allows" in {
      // Arrange
      val buffer = RingBuffer[Object](3)

      // Act
      buffer += new Object
      buffer += new Object
      buffer += new Object
      buffer += new Object

      // Assert
      buffer.length should be (3)
    }

    "overwrite the oldest item when adding an element that would exceed the limit" in {
      // Arrange
      val buffer = RingBuffer[Int](3)

      // Act
      buffer += 1
      buffer += 2
      buffer += 3
      buffer += 4

      // Assert
      buffer should contain inOrderOnly (2, 3, 4)
    }

    "correctly report the number of items it contains when it is not fully occupied" in {
      // Arrange
      val buffer = RingBuffer[Object](3)

      // Act
      buffer += new Object
      buffer += new Object

      // Assert
      buffer.length should be (2)
    }

    "correctly loop over the buffer if its limit was not reached" in {
      // Arrange
      val buffer = RingBuffer[String](3)

      // Act
      buffer += "1"
      buffer += "2"

      // Assert
      buffer should contain inOrderOnly ("1", "2")
    }

    "correctly loop over the buffer if its limit was exceeded" in {
      // Arrange
      val buffer = RingBuffer[String](3)

      // Act
      buffer += "1"
      buffer += "2"
      buffer += "3"
      buffer += "4"
      buffer += "5"

      // Assert
      buffer should contain inOrderOnly ("3", "4", "5")
    }

    "replace items in the buffer without touching other elements" in {
      // Arrange
      val buffer = RingBuffer[String](2)
      buffer += "1"
      buffer += "2"

      // Act
      buffer.update(0, "foo")

      // Assert
      buffer should contain inOrderOnly ("foo", "2")
    }

    "insert multiple elements in one call without touching other elements" in {
      // Arrange
      val buffer = RingBuffer[String](4)
      buffer += "1"
      buffer += "2"
      buffer += "3"
      buffer += "4"

      // Act
      buffer.insertAll(1, Seq("a", "b"))

      // Assert
      buffer should contain inOrderOnly ("1", "a", "b", "4")
    }

    "should return null when an element is accessed that is not yet initialised" in {
      // Arrange
      val buffer = RingBuffer[String](2)
      buffer += "1"

      // Act and
      val result = buffer(1)

      // Assert
      result shouldBe null
    }
  }
}
