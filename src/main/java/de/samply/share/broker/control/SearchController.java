package de.samply.share.broker.control;

import com.google.gson.Gson;
import de.samply.share.broker.model.DefaultInquiryCriteriaTranslatable;
import de.samply.share.broker.model.InquiryCriteriaTranslatable;
import de.samply.share.broker.model.db.tables.pojos.BankSite;
import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.InquirySite;
import de.samply.share.broker.model.db.tables.pojos.Reply;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.model.db.tables.pojos.User;
import de.samply.share.broker.monitoring.EnumReportMonitoring;
import de.samply.share.broker.monitoring.Report;
import de.samply.share.broker.monitoring.ResultList;
import de.samply.share.broker.rest.InquiryHandler;
import de.samply.share.broker.statistics.NTokenHandler;
import de.samply.share.broker.utils.db.BankSiteUtil;
import de.samply.share.broker.utils.db.BankUtil;
import de.samply.share.broker.utils.db.InquirySiteUtil;
import de.samply.share.broker.utils.db.InquiryUtil;
import de.samply.share.broker.utils.db.ReplyUtil;
import de.samply.share.broker.utils.db.SiteUtil;
import de.samply.share.common.model.dto.monitoring.StatusReportItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.tools.json.JSONArray;
import org.jooq.tools.json.JSONObject;
import org.jooq.tools.json.JSONParser;
import org.jooq.tools.json.ParseException;

/**
 * Holds methods and information necessary to create and display queries.
 */
public class SearchController {

  private static final Logger logger = LogManager.getLogger(SearchController.class);

  private static final NTokenHandler N_TOKEN_HANDLER = new NTokenHandler();

  /**
   * Release query from Icinga for bridgeheads.
   *
   * @param query     the query
   * @param sites     the sites
   * @param queryName query title name
   * @return the Inquiry ID
   */
  public static int releaseQuery(InquiryCriteriaTranslatable query, List<String> sites,
      String queryName) {
    InquiryHandler inquiryHandler = new InquiryHandler();
    int inquiryId = inquiryHandler.storeAndRelease(query, 1, queryName, "", -1, -1,
        new ArrayList<>(), true);
    List<String> siteIds = new ArrayList<>();
    if (sites.size() > 0) {
      for (String siteName : sites) {
        Site site = SiteUtil.fetchSiteByNameIgnoreCase(siteName);
        siteIds.add(site.getId().toString());
      }
    } else {
      for (Site site : SiteUtil.fetchSites()) {
        siteIds.add(site.getId().toString());
      }
    }
    inquiryHandler.setSitesForInquiry(inquiryId, siteIds);
    return inquiryId;
  }

  /**
   * release query from UI for bridgeheads.
   *
   * @param simpleQueryDtoJson the query
   * @param ntoken             the ntoken of the query
   * @param loggedUser         the logged User
   */
  public static void releaseQuery(String simpleQueryDtoJson, String ntoken, User loggedUser) {
    N_TOKEN_HANDLER.deactivateNToken(ntoken);

    InquiryHandler inquiryHandler = new InquiryHandler();
    int inquiryId = inquiryHandler
        .storeAndRelease(new DefaultInquiryCriteriaTranslatable(simpleQueryDtoJson),
            loggedUser.getId(), "", "", -1, -1, new ArrayList<>(),
            true);
    if (inquiryId > 0 && !StringUtils.isBlank(ntoken)) {
      N_TOKEN_HANDLER.saveNToken(inquiryId, ntoken, simpleQueryDtoJson);
    }

    List<String> siteIds = new ArrayList<>();
    for (Site site : SiteUtil.fetchSites()) {
      siteIds.add(site.getId().toString());
    }
    inquiryHandler.setSitesForInquiry(inquiryId, siteIds);
  }

  /**
   * get replys from the bridgeheads of the query.
   *
   * @param id the id of the query
   * @return all results as JSONObject
   */
  @SuppressWarnings("unchecked")
  public static JSONObject getReplysFromQuery(int id, boolean anonymous) {
    JSONObject replyAllSites = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    replyAllSites.put("replySites", jsonArray);

    List<Reply> replyList = new ReplyUtil().getReplyforInquriy(id);
    if (CollectionUtils.isEmpty(replyList)) {
      return replyAllSites;
    }
    JSONParser parser = new JSONParser();
    for (Reply reply : replyList) {
      if (isActiveSite(reply)) {
        try {
          JSONObject json = (JSONObject) parser.parse(reply.getContent());
          if (anonymous) {
            json.put("site", "anonymous");
          }
          jsonArray.add(json);
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }
    }

    return replyAllSites;
  }

  /**
   * Get the results for the monitoring.
   *
   * @param inquiryId the id of the query
   * @return the results of the sites
   */
  public static String getResultFromQuery(int inquiryId) throws Exception {
    ResultList resultList = new ResultList();
    List<Reply> replyList = new ReplyUtil().getReplyforInquriy(inquiryId);
    try {
      Inquiry inquiry = new InquiryUtil().fetchInquiryById(inquiryId);
      if (inquiry == null) {
        String resultJson = "{\"Success\":False,\"ExitStatus\":"
            + EnumReportMonitoring.ICINGA_STATUS_WARNING.getValue()
            + ",\"Message\":\"No query found\"}";
        return new Gson().toJson(resultJson).replace("\\", "");
      }
      for (Reply reply : replyList) {
        StatusReportItem statusReportItem = new StatusReportItem();
        statusReportItem.setExitStatus(EnumReportMonitoring.ICINGA_STATUS_OK.getValue());
        com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
        int count = parser.parse(reply.getContent()).getAsJsonObject().get("donor")
            .getAsJsonObject()
            .get("count").getAsInt();
        statusReportItem.setStatusText("count: " + count);
        Report report = new Report();
        report.setStatusReportItem(statusReportItem);
        BankSite bankSite = BankSiteUtil.fetchBankSiteByBankId(reply.getBankId());
        report.setTarget(SiteUtil.fetchSiteById(bankSite.getSiteId()).getName());
        InquirySite inquirySite = InquirySiteUtil
            .fetchInquirySiteForSiteIdAndInquiryId(bankSite.getSiteId(), inquiryId);
        report.setExecutionTime(report.calculateExecutionTime(inquirySite.getRetrievedAt(),
            reply.getRetrievedat()));
        resultList.getResultList().add(report);
      }
    } catch (Exception e) {
      throw new Exception(
          "Error while reading reply for inquiry " + inquiryId + " "
              + ". " + e);
    }
    if (replyList.size() > 0) {
      List<Report> notAnsweredBanks = checkNotAnsweredBanks(replyList, inquiryId);
      if (notAnsweredBanks.size() == 0) {
        resultList.setExitStatus(EnumReportMonitoring.ICINGA_STATUS_OK.getValue());
      } else {
        resultList.setExitStatus(EnumReportMonitoring.ICINGA_STATUS_WARNING.getValue());
      }
      resultList.getResultList().addAll(notAnsweredBanks);
      return new Gson().toJson(resultList).replace("\\", "");
    } else {
      String resultJson =
          "{\"Success\":true,\"ExitStatus\":" + EnumReportMonitoring.ICINGA_STATUS_OK.getValue()
              + ",\"Message\":\"No results yet\"}";
      return new Gson().toJson(resultJson).replace("\\", "");
    }
  }

  private static List<Report> checkNotAnsweredBanks(List<Reply> replyList, int inquiryId)
      throws Exception {
    try {
      List<Integer> idsAnswered = replyList.stream().map(Reply::getBankId)
          .collect(Collectors.toList());
      List<Integer> allIds = BankSiteUtil.fetchBankSiteBySiteIdList(
              InquirySiteUtil.fetchInquirySitesForInquiryId(inquiryId).stream()
                  .map(InquirySite::getSiteId).collect(
                      Collectors.toList())).stream()
          .map(BankSite::getBankId).collect(Collectors.toList());
      List<Integer> differences = allIds.stream()
          .filter(element -> !idsAnswered.contains(element))
          .collect(Collectors.toList());
      return createReportForNotAnsweredBanks(differences);
    } catch (Exception e) {
      throw new Exception("Error while checking not answered biobanks" + e);
    }
  }

  private static List<Report> createReportForNotAnsweredBanks(List<Integer> bankIds) {
    List<Report> reportList = new ArrayList<>();
    for (int id : bankIds) {
      logger.info("BankId that not answered: " + id);
      Report report = new Report();
      StatusReportItem statusReportItem = new StatusReportItem();
      statusReportItem.setExitStatus(EnumReportMonitoring.ICINGA_STATUS_ERROR.getValue());
      statusReportItem.setStatusText("No answer");
      report.setStatusReportItem(statusReportItem);
      report.setTarget(BankUtil.getSiteForBankId(id).getName());
      reportList.add(report);
    }
    return reportList;
  }


  private static boolean isActiveSite(Reply reply) {
    BankSite bankSite = BankSiteUtil.fetchBankSiteByBankId(reply.getBankId());
    if (bankSite == null) {
      return false;
    }

    Site site = SiteUtil.fetchSiteById(bankSite.getSiteId());
    if (site == null) {
      return false;
    }

    return site.getActive();
  }
}
