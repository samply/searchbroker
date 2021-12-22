package de.samply.share.broker.utils.db;

import de.samply.bbmri.auth.client.jwt.JwtIdToken;
import de.samply.bbmri.auth.client.jwt.JwtVocabulary;
import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.SiteDao;
import de.samply.share.broker.model.db.tables.daos.UserDao;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.model.db.tables.pojos.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for User Objects.
 *
 * @see User
 */
public final class UserUtil {

  private static final Logger logger = LoggerFactory.getLogger(UserUtil.class);

  // Prevent instantiation
  private UserUtil() {
  }

  /**
   * Update user.
   *
   * @param user the user to update
   */
  public static void updateUser(User user) {
    UserDao userDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      userDao = new UserDao(configuration);
      userDao.update(user);

    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Updates a user.
   *
   * @param user       the user to update
   * @param jwtIdToken the id token from the auth service
   * @return the updated user
   */
  public static User updateUser(User user, JwtIdToken jwtIdToken) {
    String email = getClaim(jwtIdToken, JwtVocabulary.EMAIL);
    logger.info("User changed auth id: " + email + " - is now: " + jwtIdToken.getSubject()
        + " . Was before: " + user.getAuthid());
    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      user.setAuthid(jwtIdToken.getSubject());
      user.setEmail(email);
      user.setName((getClaim(jwtIdToken, JwtVocabulary.NAME)));
      user.setUsername(email);
      UserDao userDao = new UserDao(configuration);
      userDao.update(user);
      user = userDao.fetchOneByAuthid(user.getAuthid());
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    return user;
  }

  /**
   * Creates a new user with the information from the id token.
   *
   * @param jwtIdToken the id token from the auth service
   * @return the newly created user
   */
  public static User createUser(JwtIdToken jwtIdToken) {
    User user = null;

    try (Connection conn = ResourceManager.getConnection()) {
      user = new User();
      user.setAuthid(jwtIdToken.getSubject());
      user.setEmail(getClaim(jwtIdToken, JwtVocabulary.EMAIL));
      user.setName(getClaim(jwtIdToken, JwtVocabulary.NAME));
      user.setUsername(getClaim(jwtIdToken, JwtVocabulary.EMAIL));
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      UserDao userDao = new UserDao(configuration);
      userDao.insert(user);
      user = userDao.fetchOneByAuthid(user.getAuthid());
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    return user;
  }

  /**
   * Creates or updates a user.
   *
   * @param jwtIdToken the id token from the auth service
   * @return the created or updated user
   */
  public static User createOrUpdateUser(JwtIdToken jwtIdToken) {
    User user = fetchUserByUserNameIgnoreCase(getClaim(jwtIdToken, JwtVocabulary.EMAIL));
    if (user == null) {
      return createUser(jwtIdToken);
    } else {
      return updateUser(user, jwtIdToken);
    }
  }

  /**
   * Get the site of the user.
   *
   * @param user the user
   * @return the site he belongs to
   */
  public static Site getSiteForUser(User user) {
    Site site = null;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);

      site = dslContext.select()
          .from(Tables.USER.join(Tables.USER_SITE).onKey().join(Tables.SITE).onKey())
          .where(Tables.USER.ID.equal(user.getId()))
          .fetchOneInto(Site.class);

    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
    return site;
  }

  /**
   * Get the site of the user.
   *
   * @param user the user
   * @return the id of the site he belongs to
   */
  public static Integer getSiteIdForUser(User user) {
    Site site = getSiteForUser(user);
    if (site == null) {
      return null;
    } else {
      return site.getId();
    }
  }

  /**
   * Get the site of the user.
   *
   * @param userId the id of the user
   * @return the id of the site he belongs to
   */
  public static Integer getSiteIdForUserId(int userId) {
    Site site;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);

      site = dslContext.select()
          .from(Tables.USER.join(Tables.USER_SITE).onKey().join(Tables.SITE).onKey())
          .where(Tables.USER.ID.equal(userId))
          .fetchOneInto(Site.class);

      return site.getId();

    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
    return null;
  }

  /**
   * Set the site for a user.
   *
   * @param user     the user
   * @param siteId   the id of the site
   * @param approved true if this has been verified before, false if not
   */
  public static void setSiteIdForUser(User user, int siteId, boolean approved) {
    Site site;
    SiteDao siteDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      siteDao = new SiteDao(configuration);
      site = siteDao.fetchOneById(siteId);
      if (site == null) {
        throw new RuntimeException("Site not found");
      } else {
        UserSiteUtil.setSiteForUser(user, site, approved);
      }

    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Set the site for a user.
   *
   * @param userId   the id of the user
   * @param siteId   the id of the site
   * @param approved true if this has been verified before, false if not
   */
  public static void setSiteIdForUserId(int userId, int siteId, boolean approved) {
    User user;
    UserDao userDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      userDao = new UserDao(configuration);
      user = userDao.fetchOneById(userId);
      if (user == null) {
        throw new RuntimeException("User not found");
      } else {
        setSiteIdForUser(user, siteId, approved);
      }

    } catch (SQLException e) {
      logger.error("SQL Exception caught", e);
    }
  }

  /**
   * Get all users.
   *
   * @return a list with all users
   */
  public static List<User> fetchUsers() {
    List<User> users = null;
    UserDao userDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      userDao = new UserDao(configuration);
      users = userDao.findAll();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return users;
  }

  /**
   * Fetch user by name (case insensitive).
   *
   * @param userName the user name
   * @return the user
   */
  public static User fetchUserByUserNameIgnoreCase(String userName) {
    User user = null;
    Record record;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      record = create.select().from(Tables.USER)
          .where(Tables.USER.USERNAME.equalIgnoreCase(userName)).fetchOne();
      if (record != null) {
        user = record.into(User.class);
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return user;
  }

  /**
   * Fetch user by his auth id.
   *
   * @param authId the id from the auth service
   * @return the user
   */
  public static User fetchUserByAuthId(String authId) {
    User user = null;
    UserDao userDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      userDao = new UserDao(configuration);
      user = userDao.fetchOneByAuthid(authId);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return user;
  }

  /**
   * Get the user name for a user id.
   *
   * @param userId the id of the user
   * @return the name of the user or "Unbekannt" if none found
   */
  public static String getUsernameById(int userId) {
    User user;
    UserDao userDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      userDao = new UserDao(configuration);
      user = userDao.fetchOneById(userId);
      if (user != null) {
        return user.getName();
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return "Unbekannt";
  }

  private static String getClaim(JwtIdToken jwtIdToken, String vocabulary) {
    return (String) jwtIdToken.getClaimsSet().getClaim(vocabulary);
  }
}
