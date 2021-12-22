package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.pojos.StatisticsField;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StatisticsFieldUtil {

  private static final Logger logger = LoggerFactory.getLogger(StatisticsField.class);

  private StatisticsFieldUtil() {
  }

  /**
   * Todo.
   * @param statisticsQueryIds Todo.
   * @return Todo.
   */
  public static List<StatisticsField> getFieldsByQueryIds(List<Integer> statisticsQueryIds) {
    List<StatisticsField> statisticsFields = new ArrayList<>();
    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      statisticsFields = dslContext.select()
          .from(Tables.STATISTICS_FIELD)
          .where(Tables.STATISTICS_FIELD.QUERYID.in(statisticsQueryIds))
          .fetchInto(StatisticsField.class);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
    return statisticsFields;
  }

}
