package de.samply.share.broker.listener;

import static de.samply.common.http.HttpConnector.PROXY_BYPASS_PRIVATE_NETWORKS;
import static de.samply.common.http.HttpConnector.PROXY_HTTPS_HOST;
import static de.samply.common.http.HttpConnector.PROXY_HTTPS_PASSWORD;
import static de.samply.common.http.HttpConnector.PROXY_HTTPS_PORT;
import static de.samply.common.http.HttpConnector.PROXY_HTTPS_USERNAME;
import static de.samply.common.http.HttpConnector.PROXY_HTTP_HOST;
import static de.samply.common.http.HttpConnector.PROXY_HTTP_PASSWORD;
import static de.samply.common.http.HttpConnector.PROXY_HTTP_PORT;
import static de.samply.common.http.HttpConnector.PROXY_HTTP_USERNAME;
import static de.samply.common.http.HttpConnector.USER_AGENT;
import static de.samply.share.broker.utils.Utils.getRealPath;

import de.samply.common.config.Configuration;
import de.samply.common.config.ObjectFactory;
import de.samply.common.http.HttpConnector;
import de.samply.config.util.JaxbUtil;
import de.samply.share.common.model.dto.UserAgent;
import de.samply.share.common.utils.ProjectInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

public class Proxy {

  private static final Logger logger = LogManager.getLogger(Proxy.class);

  private static final String COMMON_CONFIG_FILENAME_SUFFIX = "_common_config.xml";

  /**
   * Read the configs and return a new HttpConnector.
   * @return httpConnector
   */
  public static HttpConnector getHttpConnector() {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
      Configuration configuration = JaxbUtil
          .findUnmarshall(
              ProjectInfo.INSTANCE.getProjectName().toLowerCase() + COMMON_CONFIG_FILENAME_SUFFIX,
              jaxbContext, Configuration.class, ProjectInfo.INSTANCE.getProjectName().toLowerCase(),
              System.getProperty("catalina.base") + File.separator + "conf",
              getRealPath("/WEB-INF"));

      return getHttpConfigParams(configuration);
    } catch (FileNotFoundException e) {
      logger.error(
          "No config file found by using samply.common.config for project " + ProjectInfo.INSTANCE
              .getProjectName());
    } catch (UnmarshalException ue) {
      throw new RuntimeException("Unable to unmarshal config file");
    } catch (SAXException | JAXBException | ParserConfigurationException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static HttpConnector getHttpConfigParams(
      de.samply.common.config.Configuration configuration) {
    HashMap<String, String> configParams = new HashMap<>();

    try {
      try {
        configParams.put(PROXY_HTTP_HOST, configuration.getProxy().getHttp().getUrl().getHost());
        configParams.put(PROXY_HTTP_PORT,
            Integer.toString(configuration.getProxy().getHttp().getUrl().getPort()));
        configParams.put(PROXY_HTTP_USERNAME, configuration.getProxy().getHttp().getUsername());
        configParams.put(PROXY_HTTP_PASSWORD, configuration.getProxy().getHttp().getPassword());
      } catch (NullPointerException npe) {
        logger.debug("Could not get HTTP Proxy Settings...should be empty or null");
      }
      try {
        configParams.put(PROXY_HTTPS_HOST, configuration.getProxy().getHttps().getUrl().getHost());
        configParams.put(PROXY_HTTPS_PORT,
            Integer.toString(configuration.getProxy().getHttps().getUrl().getPort()));
        configParams.put(PROXY_HTTPS_USERNAME, configuration.getProxy().getHttps().getUsername());
        configParams.put(PROXY_HTTPS_PASSWORD, configuration.getProxy().getHttps().getPassword());
      } catch (NullPointerException npe) {
        logger.debug("Could not get HTTPS Proxy Settings...should be empty or null");
      }
      configParams.put(PROXY_BYPASS_PRIVATE_NETWORKS, Boolean.TRUE.toString());

      UserAgent userAgent = new UserAgent(ProjectInfo.INSTANCE.getProjectName(), "Samply.Share",
          ProjectInfo.INSTANCE.getVersionString());
      configParams.put(USER_AGENT, userAgent.toString());

    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    HttpConnector httpConnector = new HttpConnector(configParams);
    return httpConnector;
  }

}
