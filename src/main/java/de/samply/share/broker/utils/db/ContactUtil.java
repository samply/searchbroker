package de.samply.share.broker.utils.db;

import com.google.common.base.Joiner;
import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.ContactDao;
import de.samply.share.broker.model.db.tables.daos.UserDao;
import de.samply.share.broker.model.db.tables.pojos.Contact;
import de.samply.share.broker.model.db.tables.pojos.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for Contact Objects.
 *
 * @see Contact
 */
public final class ContactUtil {

  private static final Logger logger = LoggerFactory.getLogger(ContactUtil.class);

  // Prevent instantiation
  private ContactUtil() {
  }

  /**
   * Gets the contact for a user.
   *
   * @param user the user
   * @return the contact for the user
   */
  public static Contact getContactForUser(User user) {
    ContactDao contactDao;
    Contact contact = null;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      contactDao = new ContactDao(configuration);
      contact = contactDao.fetchOneById(user.getContactId());

    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return contact;
  }

  /**
   * Update a contact.
   *
   * @param contact the contact to update
   * @return "success" or "error" depending on the outcome
   */
  public static String updateContact(Contact contact) {
    ContactDao contactDao;
    String ret = "success";

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);

      contactDao = new ContactDao(configuration);
      contactDao.update(contact);

    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
      ret = "error";
    }
    return ret;
  }

  /**
   * Creates a contact for a user.
   *
   * @param user the user
   * @return the contact
   */
  public static Contact createContactForUser(User user) {
    ContactDao contactDao;
    Contact contact = null;
    UserDao userDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      DSLContext create = ResourceManager.getDslContext(conn);

      Record record = create.insertInto(Tables.CONTACT, Tables.CONTACT.LASTNAME).values(" ")
          .returning(Tables.CONTACT.ID).fetchOne();

      int contactId = record.getValue(Tables.INQUIRY.ID);

      userDao = new UserDao(configuration);
      user.setContactId(contactId);
      userDao.update(user);

      contactDao = new ContactDao(configuration);
      contact = contactDao.fetchOneById(contactId);

      contact.setEmail(user.getEmail());

      try {
        String[] names = user.getName().split("\\s+");
        if (names.length == 2) {
          contact.setFirstname(names[0]);
          contact.setLastname(names[1]);
        } else if (names.length == 1) {
          contact.setLastname(names[0]);
        } else if (names.length < 1) {
          logger.warn("No name provided");
        } else {
          String lastName = names[names.length - 1];
          String[] firstNames = Arrays.copyOf(names, names.length - 1);
          contact.setLastname(lastName);
          contact.setFirstname(Joiner.on(" ").join(firstNames));
        }
      } catch (Exception e) {
        logger.error("Error trying to set names to contact.", e);
      }

      contactDao.update(contact);

    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return contact;
  }
}
