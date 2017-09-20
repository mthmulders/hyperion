package hyperion.database

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend.{Database, Session}

/**
  * Allows for easy mix-in of database related stuff.
  */
trait DatabaseSupport {
  private val log = LoggerFactory.getLogger(getClass)

  private val config = ConfigFactory.load()

  log.info("Connecting to database {}", config.getString("hyperion.database.url"))
  implicit val db: Database = Database.forConfig("hyperion.database", config)

  implicit val session: Session = db.createSession()
  log.info("Database connection established: {} {}",
    session.metaData.getDatabaseProductName: Any,
    session.metaData.getDatabaseProductVersion: Any
  )
}
