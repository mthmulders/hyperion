package hyperion

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.slf4j.LoggerFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{ AnyWordSpec, AnyWordSpecLike }

abstract class BaseSpec
  extends AnyWordSpec
    with Matchers
    with TypeCheckedTripleEquals
    with Inspectors
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  protected[this] val logger = LoggerFactory.getLogger(getClass)
}