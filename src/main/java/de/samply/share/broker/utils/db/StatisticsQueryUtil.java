package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StatisticsQueryUtil {

  private static final Logger logger = LoggerFactory.getLogger(StatisticsQueryUtil.class);

  private StatisticsQueryUtil() {
  }

  /**
   * Get all the queries ids of yesterday.
   * @return the queries ids of yesterday.
   */
  public static List<Integer> getLastDayQueryIds() {
    List<Integer> queryList = new ArrayList<>();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String dateOfToday = dateFormat.format(new Date());
    String dateOfYesterday = dateFormat
        .format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      queryList = dslContext
          .fetch("SELECT " + Tables.STATISTICS_QUERY.ID + " FROM " + Tables.STATISTICS_QUERY
              + " WHERE " + Tables.STATISTICS_QUERY.CREATED + " BETWEEN '" + dateOfYesterday
              + " 00:00:00'::timestamp "
              + " AND '" + dateOfToday + " 00:00:00'::timestamp;").into(Integer.class);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
    return queryList;
  }

}
