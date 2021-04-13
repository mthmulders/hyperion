package hyperion.rest

/**
  * Helper functions for retrieving some application information
  */
object AppInfoHelper {
  private val runtime = Runtime.getRuntime

  val javaVersion: String = {
    val javaVersion = System.getProperty("java.version")
    val javaVendor = System.getProperty("java.vendor")
    s"$javaVersion ($javaVendor)"
  }

  val os: String = {
    val osName = System.getProperty("os.name")
    val osVersion = System.getProperty("os.version")
    val bits = System.getProperty("sun.arch.data.model")
    s"$osName $osVersion ($bits bits)"
  }

  def totalMem(): String = {
    humanReadableByteCount(runtime.totalMemory())
  }

  def freeMem(): String = {
    humanReadableByteCount(runtime.freeMemory())
  }

  private def humanReadableByteCount(bytes: Long): String = {
    val unit = 1024L

    if (bytes < unit) {
      s"$bytes B"
    } else {
      val exp = (Math.log(bytes.toDouble) / Math.log(unit.toDouble)).toInt
      val prefix = "KMGTPE".charAt(exp - 1)
      val amount = (bytes / Math.pow(unit.toDouble, exp)).toInt

      s"$amount ${prefix}B"
    }
  }
}