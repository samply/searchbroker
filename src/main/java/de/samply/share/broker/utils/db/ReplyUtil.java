package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.tables.daos.InquiryDao;
import de.samply.share.broker.model.db.tables.daos.ReplyDao;
import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.Reply;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

public class ReplyUtil {

  private final DonorCountExtractor countExtractor;

  public ReplyUtil() {
    this(new DonorCountExtractor());
  }

  public ReplyUtil(DonorCountExtractor countExtractor) {
    this.countExtractor = countExtractor;
  }

  /**
   * Get the reply by inquiry id.
   * @param inquiryID the inquiry id
   * @return reply
   */
  public List<Reply> getReplyforInquriy(int inquiryID) {
    List<Reply> reply = fetchReplies(inquiryID);

    reply.sort(Comparator.comparingInt(countExtractor::extractDonorCount));
    Collections.reverse(reply);

    return reply;
  }

  List<Reply> fetchReplies(int inquiryID) {
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      ReplyDao replyDao = new ReplyDao(configuration);
      return replyDao.fetchByInquiryId(inquiryID);
    } catch (SQLException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
}
