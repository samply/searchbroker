package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.EmailSiteDao;
import de.samply.share.broker.model.db.tables.pojos.EmailSite;
import java.sql.Connection;
import java.sql.SQLException;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for EmailSite Objects.
 *
 * @see EmailSite
 */
public final class EmailSiteUtil {

  private static final Logger logger = LoggerFactory.getLogger(EmailSiteUtil.class);

  // Prevent instantiation
  private EmailSiteUtil() {
  }

  /**
   * Get the site-association for an email address.
   *
   * @param email the email address
   * @return the site-association for the given email address
   */
  public static EmailSite fetchEmailSite(String email) {
    EmailSite emailSite = null;
    Record record;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      record = create.select().from(Tables.EMAIL_SITE)
          .where(Tables.EMAIL_SITE.EMAIL.equalIgnoreCase(email)).fetchOne();
      if (record != null) {
        emailSite = record.into(EmailSite.class);
      }
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
    return emailSite;
  }

  /**
   * Delete an email to site association.
   *
   * @param emailSite the email to site association to delete
   */
  protected static void deleteEmailSite(EmailSite emailSite) {
    EmailSiteDao emailSiteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      emailSiteDao = new EmailSiteDao(configuration);

      emailSiteDao.delete(emailSite);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

}
