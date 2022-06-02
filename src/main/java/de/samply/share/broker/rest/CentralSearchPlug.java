package de.samply.share.broker.rest;

import de.samply.common.mdrclient.MdrClient;
import de.samply.share.broker.model.db.tables.pojos.User;
import de.samply.share.broker.utils.db.DocumentUtil;
import de.samply.share.broker.utils.db.InquiryUtil;
import de.samply.share.broker.utils.db.UserUtil;
import de.samply.share.common.model.uiquerybuilder.QueryItem;
import de.samply.share.common.utils.Constants;
import de.samply.share.common.utils.ProjectInfo;
import de.samply.share.common.utils.QueryTreeUtil;
import de.samply.share.common.utils.QueryValidator;
import de.samply.share.common.utils.SamplyShareUtils;
import de.samply.share.common.utils.oauth2.OAuthUtils;
import de.samply.share.model.common.Query;
import de.samply.share.utils.QueryConverter;
import de.samply.web.mdrfaces.MdrContext;
import java.net.URI;
import java.util.Arrays;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.model.tree.TreeModel;

/**
 * The Class CentralSearchPlug.
 * Offers an interface to the central search platform, which allows to post queries to the broker.
 */
@Path("/cs")
public class CentralSearchPlug {

  private static final Logger logger = LogManager.getLogger(CentralSearchPlug.class);

  private static final String SERVER_HEADER_VALUE =
      "Samply.Share.Broker/" + ProjectInfo.INSTANCE.getVersionString();

  @Context
  UriInfo uriInfo;

  /**
   * Receive a POST of a search string, transmitted from central search.
   *
   * @param queryString         the serialized query from central search
   * @param authorizationHeader the authorization string as transmitted from central search
   * @param xmlNamespaceHeader  the namespace in which the posted query is
   * @return <CODE>201</CODE> when everything went well
   *        <CODE>400</CODE> on general error
   *        <CODE>403</CODE> on authentication error
   */
  @Path("/request")
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  public Response handlePostRequest(String queryString,
      @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
      @HeaderParam(Constants.HEADER_XML_NAMESPACE) String xmlNamespaceHeader) {

    boolean translateToCcpNamespace =
        SamplyShareUtils.isNullOrEmpty(xmlNamespaceHeader) || xmlNamespaceHeader
            .equalsIgnoreCase(Constants.VALUE_XML_NAMESPACE_CCP);
    logger.info(queryString);
    if (translateToCcpNamespace) {
      logger.info("Namspace Translation needed.");
    }

    // Use a new post request as a trigger to delete old tentative inquiries and unbound exposés.
    // If necessary, create a task for this.
    InquiryUtil.deleteOldTentativeInquiries();
    DocumentUtil.deleteOldUnboundDocuments();

    String userAuthId = OAuthUtils.getUserAuthId(authorizationHeader);
    User user = UserUtil.fetchUserByAuthId(userAuthId);

    if (user == null) {
      logger.warn("Invalid access token");
      return Response.status(Status.FORBIDDEN).build();
    }

    Query query;
    try {
      TreeModel<QueryItem> queryTree;
      if (translateToCcpNamespace) {
        de.samply.share.model.ccp.Query ccpQuery = QueryConverter
            .unmarshal(queryString, JAXBContext.newInstance(de.samply.share.model.ccp.Query.class),
                de.samply.share.model.ccp.Query.class);
        Query sourceQuery = QueryConverter.convertCcpQueryToCommonQuery(ccpQuery);
        queryTree = QueryTreeUtil.queryToTree(sourceQuery);
      } else {
        queryTree = QueryTreeUtil.queryToTree(QueryConverter.xmlToQuery(queryString));
      }

      MdrClient mdrClient = MdrContext.getMdrContext().getMdrClient();
      QueryValidator queryValidator = new QueryValidator(mdrClient);
      queryValidator.fixBooleans(queryTree);

      query = QueryTreeUtil.treeToQuery(queryTree);
    } catch (JAXBException e1) {
      logger.error("Failed to transform query to common namespace");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    try {
      logger.debug(QueryConverter.queryToXml(query));
    } catch (JAXBException e) {
      logger.error("Error serializing query: " + Arrays.toString(e.getStackTrace()));
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    InquiryHandler inquiryHandler = new InquiryHandler();
    int inquiryId = inquiryHandler.createTentative(query, user.getId());

    if (inquiryId < 1) {
      logger.warn("Problem while creating inquiry...");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    logger.info("New inquiry created: " + inquiryId);
    String inquiryIdS = String.valueOf(inquiryId);
    UriBuilder ub = uriInfo.getAbsolutePathBuilder();
    String baseUri = uriInfo.getBaseUri().getPath().replace("rest/", "editInquiry.xhtml");
    URI uri = ub.replacePath(baseUri).queryParam("inquiryId", inquiryIdS).build();
    String serverHeaderKey = "Server";
    return Response.status(Response.Status.CREATED).location(uri)
        .header(serverHeaderKey, SERVER_HEADER_VALUE).build();
  }
}
