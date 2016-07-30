package hyperion

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.slf4j.LoggerFactory

abstract class BaseSpec
  extends WordSpec
    with Matchers
    with TypeCheckedTripleEquals
    with Inspectors
    with WordSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  protected[this] val logger = LoggerFactory.getLogger(getClass)
}