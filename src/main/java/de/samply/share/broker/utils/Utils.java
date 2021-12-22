package de.samply.share.broker.utils;

import static de.samply.share.common.model.dto.monitoring.StatusReportItem.PARAMETER_IDM_STATUS;
import static de.samply.share.common.model.dto.monitoring.StatusReportItem.PARAMETER_IDM_VERSION;
import static de.samply.share.common.model.dto.monitoring.StatusReportItem.PARAMETER_LDM_STATUS;
import static de.samply.share.common.model.dto.monitoring.StatusReportItem.PARAMETER_LDM_VERSION;
import static de.samply.share.common.model.dto.monitoring.StatusReportItem.PARAMETER_SHARE_STATUS;
import static de.samply.share.common.model.dto.monitoring.StatusReportItem.PARAMETER_SHARE_VERSION;
import static org.omnifaces.util.Faces.getServletContext;

import com.google.gson.Gson;
import de.samply.auth.client.jwt.JwtAccessToken;
import de.samply.auth.rest.LocationDto;
import de.samply.auth.rest.UserDto;
import de.samply.share.broker.control.LocaleController;
import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.utils.connector.IcingaConnector;
import de.samply.share.broker.utils.connector.IcingaConnectorException;
import de.samply.share.broker.utils.connector.SiteReportItem;
import de.samply.share.broker.utils.db.BankUtil;
import de.samply.share.common.model.dto.SiteInfo;
import de.samply.share.common.model.dto.UserAgent;
import de.samply.share.common.model.dto.monitoring.StatusReportItem;
import de.samply.share.common.utils.AbstractConfig;
import de.samply.share.common.utils.Constants;
import de.samply.share.common.utils.ProjectInfo;
import de.samply.share.common.utils.SamplyShareUtils;
import de.samply.share.common.utils.oauth2.OAuthConfig;
import de.samply.share.common.utils.oauth2.OAuthUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.Part;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of utility methods.
 */
public class Utils {

  public static final String USER_AGENT = "http.useragent";
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);
  private static final String PROXY_REALM = "proxy.realm";
  private static final String PROXY_HTTPS_PASSWORD = "proxy.https.password";
  private static final String PROXY_HTTPS_USERNAME = "proxy.https.username";
  private static final String PROXY_HTTP_PASSWORD = "proxy.http.password";
  private static final String PROXY_HTTP_USERNAME = "proxy.http.username";
  private static final String PROXY_HTTPS_PORT = "proxy.https.port";
  private static final String PROXY_HTTPS_HOST = "proxy.https.host";
  private static final String PROXY_HTTP_PORT = "proxy.http.port";
  private static final String PROXY_HTTP_HOST = "proxy.http.host";
  private static final String XML_NAMESPACE_BASEURL = "http://schema.samply.de/";

  /**
   * Get the real path for a relative path.
   *
   * @param relativeWebPath the relative path
   * @return the real path
   */
  public static String getRealPath(String relativeWebPath) {
    ServletContext sc = ProjectInfo.INSTANCE.getServletContext();
    return sc.getRealPath(relativeWebPath);
  }

  /**
   * Gets the bank id from an authorization header.
   *
   * @param authKeyHeader the authorization header containing the api key
   * @return the bank id
   */
  public static int getBankId(String authKeyHeader) {
    int bankId = -1;
    String authKey;

    if (authKeyHeader == null || !authKeyHeader.startsWith("Samply ")) {
      return bankId;
    }
    try {
      authKey = authKeyHeader.substring(7);
    } catch (Exception e) {
      return bankId;
    }

    try (Connection connection = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(connection);

      Record r = create.select().from(Tables.BANK).join(Tables.AUTHTOKEN)
          .on(Tables.BANK.AUTHTOKEN_ID.equal(Tables.AUTHTOKEN.ID))
          .where(Tables.AUTHTOKEN.VALUE.equal(authKey)).fetchAny();

      if (r == null) {
        return bankId;
      }

      bankId = r.getValue(Tables.BANK.ID);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return bankId;
  }

  /**
   * Gets the bank id from an authorization header, if it fits the email address.
   *
   * @param authKeyHeader the authorization header
   * @param bankEmail     the bank email
   * @return the bank id if it matches the mail address, -1 if not
   */
  public static int getBankId(String authKeyHeader, String bankEmail) {
    int bankId = -1;
    String authKey;

    if (authKeyHeader == null || !authKeyHeader.startsWith("Samply ")) {
      return bankId;
    }
    try {
      authKey = authKeyHeader.substring(7);
    } catch (Exception e) {
      return bankId;
    }

    try (Connection connection = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(connection);

      Record r = create.select().from(Tables.BANK).join(Tables.AUTHTOKEN)
          .on(Tables.BANK.AUTHTOKEN_ID.equal(Tables.AUTHTOKEN.ID))
          .where(Tables.AUTHTOKEN.VALUE.equal(authKey)
              .and(Tables.BANK.EMAIL.equalIgnoreCase(bankEmail))).fetchAny();

      if (r == null) {
        return bankId;
      }

      bankId = r.getValue(Tables.BANK.ID);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    return bankId;
  }

  /**
   * Gets the locale controller.
   *
   * @return the locale controller
   */
  public static LocaleController getLocaleController() {
    return (LocaleController) FacesContext.getCurrentInstance().getApplication().getELResolver()
        .getValue(FacesContext.getCurrentInstance().getELContext(), null, "localeController");
  }

  /**
   * Gets the file name of a file part.
   *
   * @param filePart the file part
   * @return the file name
   */
  public static String getFileName(Part filePart) {
    String header = filePart.getHeader("content-disposition");
    for (String headerPart : header.split(";")) {
      if (headerPart.trim().startsWith("filename")) {
        String filepath = headerPart.substring(headerPart.indexOf('=') + 1).trim()
            .replace("\"", "");
        // in case of IE, the part name is the full path of the file
        return FilenameUtils.getName(filepath);
      }

    }
    return null;
  }

  /**
   * Save a byte array to a temporary file.
   *
   * @param prefix the prefix of the temp file
   * @param buf    the byte array to save
   * @return the resulting temp file
   */
  public static File saveByteArrayToTmpFile(String prefix, byte[] buf,
      FormDataContentDisposition contentDispositionHeader) throws IOException {
    File file = Files.createTempFile(prefix, contentDispositionHeader.getFileName()).toFile();
    FileUtils.writeByteArrayToFile(file, buf);
    return file;
  }

  /**
   * Get a list of site names from a list of sites.
   *
   * @param sites the list of sites
   * @return the list of site names
   */
  public static List<String> getSiteNames(List<Site> sites) {
    List<String> siteNames = new ArrayList<>();
    for (Site site : sites) {
      siteNames.add(site.getName());
    }
    return siteNames;
  }

  /**
   * Get the location id from the access token.
   *
   * @param accessTokenHeader the authorization header containing the access token
   * @return the location id from the first location
   */
  public static String getLocationIdFromAccessToken(String accessTokenHeader) {
    String projectName = ProjectInfo.INSTANCE.getProjectName();
    String[] fallbacks = {System.getProperty("catalina.base") + File.separator + "conf",
        getServletContext().getRealPath("/WEB-INF")};
    String authUrl =
        OAuthConfig.getOAuth2Client(projectName, fallbacks).getHost() + "/oauth2/userinfo";

    JwtAccessToken accessToken = OAuthUtils.getJwtAccessToken(accessTokenHeader);

    Client client = ClientBuilder.newClient();
    Invocation.Builder invocationBuilder = client.target(authUrl)
        .request("application/json")
        .accept("application/json").header("Authorization", accessToken.getHeader());

    UserDto userInfo = invocationBuilder.get(UserDto.class);

    List<LocationDto> locations = userInfo.getLocations();
    if (locations == null || locations.size() == 0) {
      return null;
    }

    return locations.get(0).getId();
  }

  /**
   * Get a map of settings for the http connector.
   *
   * @param config the configuration, containing the parameters
   * @return a map with http connector compliant values
   */
  public static HashMap<String, String> getHttpConfigParams(AbstractConfig config) {
    HashMap<String, String> configParams = new HashMap<>();
    configParams.put(PROXY_HTTP_HOST, config.getProperty(PROXY_HTTPS_HOST));
    configParams.put(PROXY_HTTP_PORT, config.getProperty(PROXY_HTTP_PORT));
    configParams.put(PROXY_HTTP_USERNAME, config.getProperty(PROXY_HTTP_USERNAME));
    configParams.put(PROXY_HTTP_PASSWORD, config.getProperty(PROXY_HTTP_PASSWORD));
    configParams.put(PROXY_HTTPS_HOST, config.getProperty(PROXY_HTTPS_HOST));
    configParams.put(PROXY_HTTPS_PORT, config.getProperty(PROXY_HTTPS_PORT));
    configParams.put(PROXY_HTTPS_USERNAME, config.getProperty(PROXY_HTTPS_USERNAME));
    configParams.put(PROXY_HTTPS_PASSWORD, config.getProperty(PROXY_HTTPS_PASSWORD));
    configParams.put(PROXY_REALM, config.getProperty(PROXY_REALM));

    return configParams;
  }

  /**
   * Convert User Agent String to JSON.
   *
   * @param userAgentHeader the user agent header as received via http request.
   * @param bankId          the id of the bank that sent this user agent
   * @return JSON String of the user agent information
   */
  public static String userAgentAndBankToJson(String userAgentHeader, int bankId) {
    SiteReportItem siteReportItem = new SiteReportItem();

    // {"id":12,"name":"Teststandort","versions""samply.share.client": "1.1.4-SNAPSHOT"}}
    Site site = BankUtil.getSiteForBankId(bankId);
    if (site != null) {
      siteReportItem.setId(Integer.toString(site.getId()));
      siteReportItem.setName(site.getName());
    } else {
      siteReportItem.setId("unknown");
      siteReportItem.setName("unknown");
    }

    if (userAgentHeader != null) {
      UserAgent userAgent = new UserAgent(userAgentHeader);
      if (userAgent.getShareName() != null) {
        siteReportItem.getVersions().put(userAgent.getShareName(), userAgent.getShareVersion());
      }
      if (userAgent.getLdmName() != null) {
        siteReportItem.getVersions().put(userAgent.getLdmName(), userAgent.getLdmVersion());
      }
      if (userAgent.getIdManagerName() != null) {
        siteReportItem.getVersions()
            .put(userAgent.getIdManagerName(), userAgent.getIdManagerVersion());
      }
    }
    Gson gson = new Gson();
    return gson.toJson(siteReportItem);
  }

  /**
   * Use IcingaConnector to send version information.
   *
   * @param bankId          the id of the bank that reported it
   * @param userAgentHeader the user agent header as received via http request
   */
  public static void sendVersionReportsToIcinga(int bankId, String userAgentHeader) {
    // {"id":12,"name":"Teststandort","versions""samply.share.client": "1.1.4-SNAPSHOT"}}
    Site site = BankUtil.getSiteForBankId(bankId);
    if (site == null) {
      return;
    }

    List<StatusReportItem> statusReportItems = new ArrayList<>();
    StatusReportItem statusReportItemStatus = new StatusReportItem();
    StatusReportItem statusReportItemVersion = new StatusReportItem();

    if (userAgentHeader != null) {
      UserAgent userAgent = new UserAgent(userAgentHeader);
      try {
        if (userAgent.getShareName() != null) {
          statusReportItemStatus.setExitStatus("0");
          statusReportItemStatus.setParameterName(PARAMETER_SHARE_STATUS);
          statusReportItemStatus.setStatusText("ok");

          statusReportItemVersion.setExitStatus("0");
          statusReportItemVersion.setParameterName(PARAMETER_SHARE_VERSION);
          statusReportItemVersion
              .setStatusText(userAgent.getShareName() + "/" + userAgent.getShareVersion());
        } else {
          statusReportItemStatus.setExitStatus("1");
          statusReportItemStatus.setParameterName(PARAMETER_SHARE_STATUS);
          statusReportItemStatus.setStatusText("unknown");

          statusReportItemVersion.setExitStatus("1");
          statusReportItemVersion.setParameterName(PARAMETER_SHARE_VERSION);
          statusReportItemVersion.setStatusText("unknown");
        }
        statusReportItems.add(statusReportItemStatus);
        statusReportItems.add(statusReportItemVersion);

        statusReportItemStatus = new StatusReportItem();
        statusReportItemVersion = new StatusReportItem();
        if (userAgent.getLdmName() != null) {
          statusReportItemStatus.setExitStatus("0");
          statusReportItemStatus.setParameterName(PARAMETER_LDM_STATUS);
          statusReportItemStatus.setStatusText("ok");

          statusReportItemVersion.setExitStatus("0");
          statusReportItemVersion.setParameterName(PARAMETER_LDM_VERSION);
          statusReportItemVersion
              .setStatusText(userAgent.getLdmName() + "/" + userAgent.getLdmVersion());
        } else {
          statusReportItemStatus.setExitStatus("1");
          statusReportItemStatus.setParameterName(PARAMETER_LDM_STATUS);
          statusReportItemStatus.setStatusText("unknown");

          statusReportItemVersion.setExitStatus("1");
          statusReportItemVersion.setParameterName(PARAMETER_LDM_VERSION);
          statusReportItemVersion.setStatusText("unknown");
        }

        statusReportItems.add(statusReportItemStatus);
        statusReportItems.add(statusReportItemVersion);

        statusReportItemStatus = new StatusReportItem();
        statusReportItemVersion = new StatusReportItem();
        if (userAgent.getIdManagerName() != null) {
          statusReportItemStatus.setExitStatus("0");
          statusReportItemStatus.setParameterName(PARAMETER_IDM_STATUS);
          statusReportItemStatus.setStatusText("ok");

          statusReportItemVersion.setExitStatus("0");
          statusReportItemVersion.setParameterName(PARAMETER_IDM_VERSION);
          statusReportItemVersion
              .setStatusText(userAgent.getIdManagerName() + "/" + userAgent.getIdManagerVersion());
        } else {
          statusReportItemStatus.setExitStatus("1");
          statusReportItemStatus.setParameterName(PARAMETER_IDM_STATUS);
          statusReportItemStatus.setStatusText("unknown");

          statusReportItemVersion.setExitStatus("1");
          statusReportItemVersion.setParameterName(PARAMETER_IDM_VERSION);
          statusReportItemVersion.setStatusText("unknown");
        }

        statusReportItems.add(statusReportItemStatus);
        statusReportItems.add(statusReportItemVersion);

        statusReportItemStatus = new StatusReportItem();
        statusReportItemStatus.setExitStatus("0");
        statusReportItemStatus.setParameterName("host");
        statusReportItemStatus.setStatusText("ok");

        statusReportItems.add(statusReportItemStatus);
        statusReportItems.add(statusReportItemVersion);

        IcingaConnector.reportStatusItems(site.getName(), statusReportItems);
      } catch (IcingaConnectorException e) {
        logger.warn("There was an error while trying to submit to icinga", e);
      }
    }
  }

  /**
   * Get the current Project stage as set in web.xml.
   *
   * @return ProjectStage
   */
  static ProjectStage getProjectStage() {
    return FacesContext.getCurrentInstance().getApplication().getProjectStage();
  }

  /**
   * Check if the list of sites contains only the given site.
   *
   * @param sites the list of sites to check
   * @param site  the site to check
   * @return true if the site is the only entry in the list
   */
  public static boolean onlySelf(List<String> sites, Integer site) {
    return (sites != null && site != null && sites.size() == 1 && sites.contains(site.toString()));
  }

  /**
   * Convert a site pojo to a site info object.
   *
   * @param site the site pojo
   * @return the site info object
   */
  public static SiteInfo siteToSiteInfo(Site site) {
    SiteInfo siteInfo = new SiteInfo();
    siteInfo.setContact(site.getContact());
    siteInfo.setDescription(site.getDescription());
    siteInfo.setId(site.getId());
    siteInfo.setName(site.getName());
    siteInfo.setNameExtended(site.getNameExtended());
    return siteInfo;
  }

  /**
   * Fixes the namespaces of serialized objects for external requests, depending on the project
   * context. Internally, the common schemas are used for serializing XML.
   *
   * @param in              serialized object as XML string
   * @param targetNamespace desired output namespace (will be converted to lower case)
   * @return String with namespaces depending on project context
   */
  public static String fixNamespaces(String in, String targetNamespace) {

    if (SamplyShareUtils.isNullOrEmpty(in)) {
      return "";
    } else if (SamplyShareUtils.isNullOrEmpty(targetNamespace)) {
      // For older clients (<2.0.0-RC5) that don't supply a target namespace, use the project name
      // (or "ccp" for project "dktk")
      String fallbackNamespace = ProjectInfo.INSTANCE.getProjectName().toLowerCase();
      if (fallbackNamespace.equalsIgnoreCase("dktk")) {
        return in.replace(XML_NAMESPACE_BASEURL + Constants.VALUE_XML_NAMESPACE_COMMON,
            XML_NAMESPACE_BASEURL + Constants.VALUE_XML_NAMESPACE_CCP);
      } else {
        return in.replace(XML_NAMESPACE_BASEURL + Constants.VALUE_XML_NAMESPACE_COMMON,
            XML_NAMESPACE_BASEURL + fallbackNamespace);
      }
    } else if (!targetNamespace.equalsIgnoreCase(Constants.VALUE_XML_NAMESPACE_COMMON)) {
      return in.replace(XML_NAMESPACE_BASEURL + Constants.VALUE_XML_NAMESPACE_COMMON,
          XML_NAMESPACE_BASEURL + targetNamespace.toLowerCase());
    } else {
      return in;
    }
  }
}
