package de.samply.share.broker.utils.db;

import de.samply.auth.rest.LocationDto;
import de.samply.auth.rest.LocationListDto;
import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.SiteDao;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.utils.Utils;
import de.samply.share.common.utils.SamplyShareUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for Site Objects.
 *
 * @see Site
 */
public final class SiteUtil {

  private static final Logger logger = LoggerFactory.getLogger(SiteUtil.class);

  // Prevent instantiation
  private SiteUtil() {
  }

  /**
   * Insert sites to the database.
   *
   * @param locationList The sites (aka locations), as received from the authentication service
   */
  public static void insertSites(LocationListDto locationList) {
    List<Site> persistedSites = fetchSites();
    List<Site> newSites = new ArrayList<>();
    List<String> siteNames = Utils.getSiteNames(persistedSites);

    for (LocationDto location : locationList.getLocations()) {
      logger.debug(location.getId());
      if (siteNames.contains(location.getId())) {
        continue;
      }
      Site newSite = new Site();
      newSite.setContact(location.getContact());
      newSite.setDescription(location.getDescription());
      newSite.setName(location.getId());
      newSite.setNameExtended(location.getName());
      newSites.add(newSite);
    }

    SiteDao siteDao;
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      siteDao = new SiteDao(configuration);
      siteDao.insert(newSites);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
  }

  /**
   * Get all sites.
   *
   * @return a list with all sites
   */
  public static List<Site> fetchSites() {
    List<Site> sites = null;
    SiteDao siteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      siteDao = new SiteDao(configuration);
      sites = siteDao.findAll();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return sites;
  }

  /**
   * Get all sites an inquiry is distributed to.
   *
   * @param inquiryId the id of the inquiry
   * @return a list of sites where this inquiry is distributed to
   */
  public static List<Site> fetchSitesForInquiry(int inquiryId) {
    List<Site> sites = null;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      sites = create.select()
          .from(Tables.INQUIRY.join(Tables.INQUIRY_SITE).onKey().join(Tables.SITE).onKey())
          .where((Tables.INQUIRY.ID).equal(inquiryId)).fetchInto(Site.class);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return sites;
  }

  /**
   * Get a site by its (short) name.
   *
   * @param name (short) name of the site
   * @return the site
   */
  public static Site fetchSiteByName(String name) {
    Site site = null;
    SiteDao siteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      siteDao = new SiteDao(configuration);
      site = siteDao.fetchOneByName(name);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return site;
  }

  /**
   * Get a site by its id.
   *
   * @param id the id of the site
   * @return the site
   */
  public static Site fetchSiteById(int id) {
    Site site = null;
    SiteDao siteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      siteDao = new SiteDao(configuration);
      site = siteDao.fetchOneById(id);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return site;
  }

  /**
   * Get a site by its (short) name - case insensitive.
   *
   * @param siteName (short) name of the site
   * @return the site
   */
  public static Site fetchSiteByNameIgnoreCase(String siteName) {
    Site site = null;
    Record record;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      record = create.select().from(Tables.SITE).where(Tables.SITE.NAME.equalIgnoreCase(siteName))
          .fetchOne();
      if (record != null) {
        site = record.into(Site.class);
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return site;
  }

  /**
   * Get a list of site names for a list of site ids.
   *
   * @param siteIdList a list of site ids
   * @return the list of site names in the same order as the ids were
   */
  public static List<String> getSiteNamesByIds(List<String> siteIdList) {
    List<Site> sites;
    SiteDao siteDao;
    List<String> siteNames = new ArrayList<>();

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      siteDao = new SiteDao(configuration);

      sites = siteDao.fetchById(SamplyShareUtils.convertStringListToIntegerArray(siteIdList));
      for (Site site : sites) {
        siteNames.add(site.getName());
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return siteNames;
  }

  /**
   * Insert new site.
   * @param site the new site
   */
  public static void insertNewSite(Site site) {
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      SiteDao siteDao = new SiteDao(configuration);
      siteDao.insert(site);
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

}
