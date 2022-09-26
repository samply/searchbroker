package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.enums.InquiryCriteriaType;
import de.samply.share.broker.model.db.tables.daos.InquiryCriteriaDao;
import de.samply.share.broker.model.db.tables.pojos.InquiryCriteria;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

public class InquiryCriteriaUtil {

  /**
   * Get the inquiry criteria by a inquiry id.
   * @param inquiryId the inquiry id
   * @return a list of inquiry criteria
   */
  public static List<InquiryCriteria> fetchInquiryCriteriaForInquiryId(int inquiryId) {
    List<InquiryCriteria> inquiryCriteria;
    InquiryCriteriaDao inquiryCriteriaDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      inquiryCriteriaDao = new InquiryCriteriaDao(configuration);

      inquiryCriteria = inquiryCriteriaDao.fetchByInquiryId(inquiryId);
      return inquiryCriteria;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
  }

  /**
   * Get the inquiry criteria with the typ CQL by inquiry id.
   * @param inquiryId the inquiry id
   * @return a list of inquiry criteria with typ CQL
   */
  public static List<InquiryCriteria> fetchCriteriaListForInquiryIdTypeCql(int inquiryId) {
    List<InquiryCriteria> inquiryCriteria = fetchInquiryCriteriaForInquiryId(inquiryId);

    return inquiryCriteria.stream()
        .filter(inquiryCriteriaTemp -> inquiryCriteriaTemp.getType() == InquiryCriteriaType.IC_CQL)
        .collect(Collectors.toList());
  }

  /**
   * Get the inquiry criteria with the typ Query by inquiry id.
   * @param inquiryId the inquiry id
   * @return a list of inquiry criteria with typ Query
   */
  public static String fetchCriteriaForInquiryIdTypeQuery(int inquiryId) {
    List<InquiryCriteria> inquiryCriteria = fetchInquiryCriteriaForInquiryId(inquiryId);

    Optional<InquiryCriteria> inquiryCriteriaOptional =
        inquiryCriteria.stream()
            .filter(inquiryCriteriaTemp -> inquiryCriteriaTemp.getType()
                == InquiryCriteriaType.IC_QUERY)
            .findFirst();

    return inquiryCriteriaOptional.isPresent() ? inquiryCriteriaOptional.get().getCriteria() : "";
  }

  /**
   * Get the inquiry criteria with the typ structured query by inquiry id.
   *
   * @param inquiryId the inquiry id
   * @return inquiry criteria with typ structured query
   */
  public static String fetchCriteriaListForInquiryIdTypeStructuredQuery(
      int inquiryId) {
    List<InquiryCriteria> inquiryCriteria = fetchInquiryCriteriaForInquiryId(inquiryId);

    Optional<InquiryCriteria> inquiryCriteriaOptional =
        inquiryCriteria.stream()
            .filter(inquiryCriteriaTemp -> inquiryCriteriaTemp.getType()
                == InquiryCriteriaType.IC_STRUCTURED_QUERY)
            .findFirst();

    return inquiryCriteriaOptional.isPresent() ? inquiryCriteriaOptional.get().getCriteria() : "";
  }

  /**
   * Update the inquiry criteria.
   * @param inquiryCriteria the updated inquiry criteria
   */
  public static void updateInquiryCriteria(InquiryCriteria inquiryCriteria) {
    InquiryCriteriaDao inquiryCriteriaDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      inquiryCriteriaDao = new InquiryCriteriaDao(configuration);
      inquiryCriteriaDao.update(inquiryCriteria);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

}
