package de.samply.share.broker.statistics;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.NtokenQueryDao;
import de.samply.share.broker.model.db.tables.pojos.NtokenQuery;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;


public class NTokenHandler {

  public static final int INQUIRY_ID_FOR_ERROR = -1;
  private static final Logger logger = LogManager.getLogger(NTokenHandler.class);

  /**
   * Todo.
   * @param inquiryId Todo.
   * @param ntoken Todo.
   * @param query Todo.
   * @return Todo.
   */
  public int saveNToken(int inquiryId, String ntoken, String query) {
    try (Connection connection = ResourceManager.getConnection()) {
      NtokenQuery ntokenQuery = new NtokenQuery();
      ntokenQuery.setNtoken(ntoken);
      ntokenQuery.setInquiryid(inquiryId);
      ntokenQuery.setQuery(query);
      ntokenQuery.setWascreated(new Timestamp(new Date().getTime()));

      DSLContext dslContext = ResourceManager.getDslContext(connection);

      dslContext
          .insertInto(Tables.NTOKEN_QUERY,
              Tables.NTOKEN_QUERY.INQUIRYID,
              Tables.NTOKEN_QUERY.NTOKEN,
              Tables.NTOKEN_QUERY.QUERY,
              Tables.NTOKEN_QUERY.ACTIVE,
              Tables.NTOKEN_QUERY.WASCREATED)
          .values(ntokenQuery.getInquiryid(),
              ntokenQuery.getNtoken(),
              ntokenQuery.getQuery(),
              true,
              ntokenQuery.getWascreated())
          .execute();

      return dslContext.lastID().intValue();
    } catch (SQLException e) {
      e.printStackTrace();
      return -1;
    }
  }

  /**
   * Todo.
   * @param ntoken Todo.
   * @return Todo.
   */
  public String findLatestQuery(String ntoken) {
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      NtokenQueryDao ntokenQueryDao = new NtokenQueryDao(configuration);
      List<NtokenQuery> ntokenQueries = ntokenQueryDao.fetch(Tables.NTOKEN_QUERY.NTOKEN, ntoken);

      if (ntokenQueries.isEmpty()) {
        return null;
      }

      Optional<NtokenQuery> latestNTokenQueryOptional =
          ntokenQueries.stream().max(Comparator.comparing(NtokenQuery::getWascreated));

      return latestNTokenQueryOptional.get().getQuery();
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }

    return null;
  }

  /**
   * Todo.
   * @param ntoken Todo.
   * @return Todo.
   */
  public int findLatestInquiryId(String ntoken) {
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      NtokenQueryDao ntokenQueryDao = new NtokenQueryDao(configuration);
      List<NtokenQuery> ntokenQueries = ntokenQueryDao.fetch(Tables.NTOKEN_QUERY.NTOKEN, ntoken);

      if (ntokenQueries.isEmpty()) {
        return INQUIRY_ID_FOR_ERROR;
      }

      Optional<NtokenQuery> latestNTokenQueryOptional =
          ntokenQueries.stream().max(Comparator.comparing(NtokenQuery::getWascreated));

      NtokenQuery ntokenQuery = latestNTokenQueryOptional.get();

      return BooleanUtils.isTrue(ntokenQuery.getActive()) ? ntokenQuery.getInquiryid() : -1;
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }

    return -1;
  }

  /**
   * Todo.
   * @param ntoken Todo.
   */
  public void deactivateNToken(String ntoken) {
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      NtokenQueryDao ntokenQueryDao = new NtokenQueryDao(configuration);
      List<NtokenQuery> ntokenQueries = ntokenQueryDao.fetch(Tables.NTOKEN_QUERY.NTOKEN, ntoken);

      if (ntokenQueries.isEmpty()) {
        return;
      }

      Optional<NtokenQuery> latestNTokenQueryOptional =
          ntokenQueries.stream().max(Comparator.comparing(NtokenQuery::getWascreated));

      //noinspection ConstantConditions
      if (!latestNTokenQueryOptional.isPresent()) {
        return;
      }

      NtokenQuery ntokenQuery = latestNTokenQueryOptional.get();
      ntokenQuery.setActive(false);
      ntokenQueryDao.update(ntokenQuery);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Save the selected biobanks in the database.
   * @param ntoken the nToken
   * @param selectedBiobank the selected biobanks
   * @throws NotFoundException if the query was not found by the nToken
   */
  public void saveSelectedBiobank(String ntoken, String selectedBiobank) throws NotFoundException {
    try (Connection connection = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(connection)
          .set(SQLDialect.POSTGRES);
      NtokenQueryDao ntokenQueryDao = new NtokenQueryDao(configuration);
      List<NtokenQuery> ntokenQueries = ntokenQueryDao.fetch(Tables.NTOKEN_QUERY.NTOKEN, ntoken);
      if (ntokenQueries.isEmpty()) {
        throw new NotFoundException("Not found the query by the given n-Token");
      }
      Optional<NtokenQuery> latestNTokenQueryOptional =
              ntokenQueries.stream().max(Comparator.comparing(NtokenQuery::getWascreated));
      NtokenQuery ntokenQuery = latestNTokenQueryOptional.get();
      ntokenQuery.setSelectedBiobank(selectedBiobank);
      ntokenQueryDao.update(ntokenQuery);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Get the selected biobanks by the nToken.
   * @param ntoken n-Token
   * @return the selected biobanks
   * @throws SQLException exception in the database
   */
  public String getSelectedBiobank(String ntoken) throws SQLException {
    Connection connection = ResourceManager.getConnection();
    Configuration configuration = new DefaultConfiguration().set(connection)
        .set(SQLDialect.POSTGRES);
    NtokenQueryDao ntokenQueryDao = new NtokenQueryDao(configuration);
    List<NtokenQuery> ntokenQueries = ntokenQueryDao.fetch(Tables.NTOKEN_QUERY.NTOKEN, ntoken);
    if (ntokenQueries.isEmpty()) {
      throw new NotFoundException("Not found the query by the given n-Token");
    }
    Optional<NtokenQuery> latestNTokenQueryOptional =
            ntokenQueries.stream().max(Comparator.comparing(NtokenQuery::getWascreated));
    NtokenQuery ntokenQuery = latestNTokenQueryOptional.get();
    return ntokenQuery.getSelectedBiobank();
  }
}
