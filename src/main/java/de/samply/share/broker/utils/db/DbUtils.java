package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.tables.daos.UserDao;
import java.sql.Connection;
import java.sql.SQLException;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Offers some helper methods for db access.
 */
public class DbUtils {

  private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);

  /**
   * Check if the database is accessible.
   * Try to read from User Table.
   *
   * @return true if accessible
   */
  public static boolean checkConnection() {
    boolean success = true;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      UserDao userDao = new UserDao(configuration);
      userDao.findAll();
    } catch (SQLException e) {
      logger.warn("SQL Exception while trying to read user table.", e);
      success = false;
    }
    return success;
  }

}
