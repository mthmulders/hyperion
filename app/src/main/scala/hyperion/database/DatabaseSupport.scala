package hyperion.database

import slick.jdbc.JdbcBackend.{Database, Session}

/**
  * Allows for easy mix-in of database related stuff.
  */
trait DatabaseSupport {
  implicit val db = Database.forConfig("hyperion.database")

  implicit val session: Session = db.createSession()
}
