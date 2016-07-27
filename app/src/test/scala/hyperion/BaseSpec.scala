package hyperion

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Inspectors, Matchers, WordSpec}
import org.slf4j.LoggerFactory

abstract class BaseSpec extends WordSpec with Matchers with TypeCheckedTripleEquals with Inspectors {
  protected[this] val logger = LoggerFactory.getLogger(getClass)
}