package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.pojos.Tokenrequest;
import de.samply.share.common.utils.SamplyShareUtils;
import java.sql.Connection;
import java.sql.SQLException;
import org.jooq.DSLContext;
import org.jooq.DatePart;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for TokenRequest Objects.
 *
 * @see Tokenrequest
 */
public final class TokenRequestUtil {

  private static final Logger logger = LoggerFactory.getLogger(TokenRequestUtil.class);

  // Prevent instantiation
  private TokenRequestUtil() {
  }

  // Delete Token Requests older than 7 days and those with a future date

  /**
   * Delete old token requests.
   */
  public static void deleteOldTokenRequests() {
    int affectedRows = 0;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      affectedRows = create.delete(Tables.TOKENREQUEST)
          .where(DSL.currentTimestamp()
              .greaterThan(DSL.timestampAdd(Tables.TOKENREQUEST.ISSUED, 7, DatePart.DAY)))
          .or(Tables.TOKENREQUEST.ISSUED.greaterThan(DSL.currentTimestamp()))
          .execute();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    if (affectedRows > 0) {
      logger.info("Deleted " + affectedRows + " old tokenRequests");
    }
  }

  /**
   * Creates the token request for email.
   *
   * @param email the email
   * @return the tokenrequest
   */
  public static Tokenrequest createTokenRequestForEmail(String email) {
    Tokenrequest tr = new Tokenrequest();
    tr.setAuthcode(SamplyShareUtils.createRandomDigitString(8));
    tr.setEmail(email);

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      Record record =
          create.insertInto(Tables.TOKENREQUEST, Tables.TOKENREQUEST.AUTHCODE,
              Tables.TOKENREQUEST.EMAIL)
              .values(tr.getAuthcode(), tr.getEmail())
              .returning(Tables.TOKENREQUEST.ID, Tables.TOKENREQUEST.ISSUED)
              .fetchOne();

      tr.setId(record.getValue(Tables.TOKENREQUEST.ID));
      tr.setIssued(record.getValue(Tables.TOKENREQUEST.ISSUED));

    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return tr;
  }
}
