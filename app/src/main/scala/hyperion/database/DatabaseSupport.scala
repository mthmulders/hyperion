package hyperion.database

import akka.event.slf4j.Slf4jLogger
import com.typesafe.config.ConfigFactory
import slick.jdbc.JdbcBackend.{Database, Session}

/**
  * Allows for easy mix-in of database related stuff.
  */
trait DatabaseSupport extends Slf4jLogger {
  private val config = ConfigFactory.load()

  log.info("Connecting to database {}", config.getString("hyperion.database.url"))
  implicit val db: Database = Database.forConfig("hyperion.database", config)

  implicit val session: Session = db.createSession()
  log.info("Database connection established: {} {}",
    session.metaData.getDatabaseProductName: Any,
    session.metaData.getDatabaseProductVersion: Any
  )
}
