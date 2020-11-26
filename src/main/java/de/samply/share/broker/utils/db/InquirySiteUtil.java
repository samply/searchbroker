package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.tables.daos.InquirySiteDao;
import de.samply.share.broker.model.db.tables.pojos.InquirySite;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

/**
 * This class provides static methods for CRUD operations for InquirySite Objects.
 *
 * @see InquirySite
 */
public class InquirySiteUtil {

  private static final Logger logger = LogManager.getLogger(InquirySiteUtil.class);

  // Prevent instantiation
  private InquirySiteUtil() {

  }

  /**
   * Update an inquiry to site association.
   *
   * @param inquirySite the inquiry site association to update
   */
  public static void updateInquirySite(InquirySite inquirySite) {
    InquirySiteDao inquirySiteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      inquirySiteDao = new InquirySiteDao(configuration);
      inquirySiteDao.update(inquirySite);
    } catch (SQLException e) {
      logger.error("Error while trying to update InquirySite.", e);
    }
  }

  /**
   * Get all site associations for a given inquiry.
   *
   * @param inquiryId the id of the inquiry
   * @return a list of all inquiry to site associations belonging to that inquiry
   */
  public static List<InquirySite> fetchInquirySitesForInquiryId(int inquiryId) {
    List<InquirySite> inquirySites;
    InquirySiteDao inquirySiteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      inquirySiteDao = new InquirySiteDao(configuration);

      inquirySites = inquirySiteDao.fetchByInquiryId(inquiryId);
      return inquirySites;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
  }

}
