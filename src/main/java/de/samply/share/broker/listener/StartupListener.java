package de.samply.share.broker.listener;

import de.samply.common.http.HttpConnector;
import de.samply.common.mdrclient.MdrClient;
import de.samply.config.util.FileFinderUtil;
import de.samply.share.broker.jobs.SpawnJob;
import de.samply.share.broker.utils.Config;
import de.samply.share.broker.utils.db.Migration;
import de.samply.share.common.utils.ProjectInfo;
import de.samply.web.mdrfaces.MdrContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The listener interface for receiving startup events. The class that is interested in processing a
 * startup event implements this interface, and the object created with that class is registered
 * with a component using the component's <code>addStartupListener</code> method. When the startup
 * event occurs, that object's appropriate method is invoked.
 */
public class StartupListener implements javax.servlet.ServletContextListener {

  /**
   * The Constant logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);
  private SchedulerFactory sf = new StdSchedulerFactory();

  /* (non-Javadoc)
   * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
   */
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // This manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory
    // leaks to this class
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      try {
        DriverManager.deregisterDriver(driver);
        logger.info("Deregistering jdbc driver: " + driver);
        for (Scheduler scheduler : sf.getAllSchedulers()) {
          scheduler.shutdown();
        }
      } catch (SQLException e) {
        logger.error("Error deregistering driver:" + driver + "\n" + e.getMessage());
      } catch (SchedulerException e) {
        logger.error(e.getMessage(),e);
      }
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
   */
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ProjectInfo.INSTANCE.initProjectMetadata(sce);
    logger.info("Loading Samply.Share.Broker v" + ProjectInfo.INSTANCE.getVersionString() + " for "
        + ProjectInfo.INSTANCE.getProjectName());
    logger.debug("Listener to the web application startup is running. Checking configuration...");
    Config c = Config.getInstance(System.getProperty("catalina.base") + File.separator + "conf",
        sce.getServletContext().getRealPath("/WEB-INF"));
    ProjectInfo.INSTANCE.setConfig(c);
    try {
      Configurator.initialize(
          null,
          FileFinderUtil.findFile(
              "log4j2.xml", ProjectInfo.INSTANCE.getProjectName(),
              System.getProperty("catalina.base") + File.separator + "conf",
              sce.getServletContext().getRealPath("/WEB-INF")).getAbsolutePath());
    } catch (FileNotFoundException e) {
      logger.error(e.getMessage(),e);
    }
    Migration.doUpgrade();
    String mdrUrl = c.getProperty("mdr.url");
    HttpConnector httpConnector = Proxy.getHttpConnector();
    MdrClient mdrClient = new MdrClient(mdrUrl, httpConnector.getJerseyClient(mdrUrl));
    MdrContext.getMdrContext().init(mdrClient);
    SpawnJob spawnJob = new SpawnJob();
    spawnJob.spawnStatisticJob();
  }

}
