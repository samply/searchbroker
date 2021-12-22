package de.samply.share.broker.utils.db;

import de.samply.auth.rest.LocationDto;
import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.tables.daos.SiteDao;
import de.samply.share.broker.model.db.tables.daos.UserSiteDao;
import de.samply.share.broker.model.db.tables.pojos.EmailSite;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.model.db.tables.pojos.User;
import de.samply.share.broker.model.db.tables.pojos.UserSite;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for UserSite Objects.
 *
 * @see UserSite
 */
public final class UserSiteUtil {

  private static final Logger logger = LoggerFactory.getLogger(UserSiteUtil.class);

  // Prevent instantiation
  private UserSiteUtil() {
  }

  /**
   * Get the user to site association for a user.
   *
   * @param user the user
   * @return the user to site association for that user
   */
  public static UserSite fetchUserSiteByUser(User user) {
    List<UserSite> userSites;
    UserSiteDao userSiteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      userSiteDao = new UserSiteDao(configuration);

      userSites = userSiteDao.fetchByUserId(user.getId());
      // There shall be but one assignment
      if (userSites != null && userSites.size() == 1) {
        return userSites.get(0);
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return null;
  }

  /**
   * Set the user to site association for a user.
   *
   * @param user     the user
   * @param site     the site to link the user to
   * @param approved true if this has been verified before, false if it is tentative
   * @return the user to site association for that user
   */
  public static UserSite setSiteForUser(User user, Site site, boolean approved) {
    UserSite newUserSite = null;
    UserSite oldUserSite = null;
    UserSiteDao userSiteDao;

    if (user == null) {
      logger.warn("Tried to set site for user null");
      return null;
    }

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      userSiteDao = new UserSiteDao(configuration);
      List<UserSite> userSites = userSiteDao.fetchByUserId(user.getId());

      if (userSites != null && !userSites.isEmpty()) {
        oldUserSite = userSites.get(0);
        userSiteDao.delete(oldUserSite);
      }

      if (site == null) {
        logger.debug("New site is null. Just deleted the old one.");
        return null;
      }

      newUserSite = new UserSite();
      newUserSite.setApproved(approved);
      newUserSite.setSiteId(site.getId());
      newUserSite.setUserId(user.getId());
      userSiteDao.insert(newUserSite);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
    return newUserSite;
  }

  /**
   * Update a user to site association.
   *
   * @param userSite the user to site association to update
   */
  public static void updateUserSite(UserSite userSite) {
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      UserSiteDao userSiteDao = new UserSiteDao(configuration);
      userSiteDao.update(userSite);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Check preemptive association table and assign site if necessary.
   *
   * @param user the user to check the assignment for
   */
  public static void createUserSiteAssociation(User user) {
    if (user == null || user.getEmail() == null) {
      return;
    }
    EmailSite emailSite = EmailSiteUtil.fetchEmailSite(user.getEmail());
    if (emailSite == null || emailSite.getId() == null || emailSite.getSite() == null
        || emailSite.getSite() < 1) {
      return;
    }

    Integer siteIdForUser = UserUtil.getSiteIdForUser(user);
    if (siteIdForUser != null) {
      logger.info(
          "Reassigning site for user " + user.getId() + ". Was " + siteIdForUser + " - will be "
              + emailSite.getSite());
    } else {
      logger.info("Assigning site " + emailSite.getSite() + " to user " + user.getId());
    }
    UserUtil.setSiteIdForUser(user, emailSite.getSite(), Boolean.TRUE);
    EmailSiteUtil.deleteEmailSite(emailSite);
  }

  /**
   * Delete a user to site association.
   *
   * @param userSite the user to site association to delete
   */
  private static void deleteUserSite(UserSite userSite) {
    UserSiteDao userSiteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      userSiteDao = new UserSiteDao(configuration);

      userSiteDao.delete(userSite);
    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Delete the site assignment of a user.
   *
   * @param user the user
   */
  public static void deleteSiteFromUser(User user) {
    UserSite userSite = fetchUserSiteByUser(user);
    if (userSite != null) {
      deleteUserSite(userSite);
    }
  }

  /**
   * Update the site assignments for a user.
   *
   * @param user          the user to add the sites to
   * @param userLocations the list of sites to add, as received from the authentication service
   */
  public static void updateUserLocations(User user, List<LocationDto> userLocations) {
    if (userLocations == null || userLocations.size() < 1) {
      logger.warn("User " + user.getAuthid() + " is not associated with any locations.");
    } else {
      if (userLocations.size() > 1) {
        logger
            .warn("User " + user.getAuthid() + " has more that one location. Using the first one.");
      }
      LocationDto userLocation = userLocations.get(0);
      logger.debug(
          "User " + user.getAuthid() + " is associated with location " + userLocation.getId());

      SiteDao siteDao = null;
      Site site = null;
      try (Connection connection = ResourceManager.getConnection()) {
        Configuration configuration = new DefaultConfiguration().set(connection)
            .set(SQLDialect.POSTGRES);
        siteDao = new SiteDao(configuration);
        site = siteDao.fetchOneByName(userLocation.getId());
        Integer siteIdForUser = UserUtil.getSiteIdForUser(user);
        if (!Objects.equals(site.getId(), siteIdForUser)) {
          logger.debug("Updating user location. " + siteIdForUser + " -> " + site.getId());
          setSiteForUser(user, site, true);
        }
      } catch (SQLException e) {
        logger.error(e.getMessage(),e);
        throw new RuntimeException(e);
      }

    }
  }
}
