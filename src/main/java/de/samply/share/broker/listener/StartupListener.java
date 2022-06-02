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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

/**
 * The listener interface for receiving startup events. The class that is interested in processing a
 * startup event implements this interface, and the object created with that class is registered
 * with a component using the component's <code>addStartupListener</code> method. When the startup
 * event occurs, that object's appropriate method is invoked.
 */
public class StartupListener implements javax.servlet.ServletContextListener {

  private static final Logger LOGGER = LogManager.getLogger(StartupListener.class);

  private final SchedulerFactory sf = new StdSchedulerFactory();

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
        LOGGER.info("Deregistering jdbc driver: " + driver);
        for (Scheduler scheduler : sf.getAllSchedulers()) {
          scheduler.shutdown();
        }
      } catch (SQLException e) {
        LOGGER.fatal("Error deregistering driver:" + driver + "\n" + e.getMessage());
      } catch (SchedulerException e) {
        e.printStackTrace();
      }
    }
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
   */
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ProjectInfo.INSTANCE.initProjectMetadata(sce);
    LOGGER.info("Loading Samply.Share.Broker v" + ProjectInfo.INSTANCE.getVersionString() + " for "
        + ProjectInfo.INSTANCE.getProjectName());
    LOGGER.debug("Listener to the web application startup is running. Checking configuration...");
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
      e.printStackTrace();
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
