package de.samply.share.broker.rest;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.AuthtokenDao;
import de.samply.share.broker.model.db.tables.daos.BankDao;
import de.samply.share.broker.model.db.tables.daos.BankSiteDao;
import de.samply.share.broker.model.db.tables.daos.TokenrequestDao;
import de.samply.share.broker.model.db.tables.pojos.Authtoken;
import de.samply.share.broker.model.db.tables.pojos.Bank;
import de.samply.share.broker.model.db.tables.pojos.BankSite;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.model.db.tables.pojos.Tokenrequest;
import de.samply.share.broker.model.db.tables.records.TokenrequestRecord;
import de.samply.share.broker.utils.MailUtils;
import de.samply.share.broker.utils.db.BankSiteUtil;
import de.samply.share.broker.utils.db.SiteUtil;
import de.samply.share.broker.utils.db.TokenRequestUtil;
import de.samply.share.common.utils.SamplyShareUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the whole management of connected banks. Registration, activation, deletion.
 */
// TODO: make everything static here?
public class BankRegistration {

  /**
   * The log4j logger.
   */
  private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  /**
   * Instantiates a new bank registration object.
   */
  public BankRegistration() {
  }

  /**
   * Register a new bank.
   *
   * @param email the email address of the new bank
   * @return <CODE>401</CODE> if a new request was issued and authorization is required
   * <CODE>409</CODE> if another bank with the same email is already registered
   */
  protected Response.Status register(String email) {
    Response.Status responseStatus;
    TokenrequestDao tokenrequestDao;
    Tokenrequest tokenRequest;
    List<Tokenrequest> tokenRequestList = null;

    try (Connection connection = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(connection)
          .set(SQLDialect.POSTGRES);
      tokenrequestDao = new TokenrequestDao(configuration);
      tokenRequestList = tokenrequestDao.fetchByEmail(email);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    if (tokenRequestList != null && tokenRequestList.size() > 0) {
      logger.info("Re-registration was attempted for " + email);
      responseStatus = Response.Status.CONFLICT;
    } else {
      logger.info("Creating new tokenrequest for " + email);
      // 1 Create Token
      tokenRequest = TokenRequestUtil.createTokenRequestForEmail(email);
      // 2 send email
      MailUtils.sendActivationmail(email, tokenRequest.getAuthcode());
      // 3 send 401
      responseStatus = Response.Status.UNAUTHORIZED;
    }

    return responseStatus;
  }

  /**
   * Activate a bank.
   *
   * @param email    the email address of the bank to activate
   * @param authCode the request token provided
   * @return a string representation of the outcome
   */
  protected String activate(String email, String authCode, String locationId) {
    String returnValue = "error";
    TokenrequestRecord tokenRequestRecord = null;

    try (Connection connection = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(connection);

      tokenRequestRecord = dslContext.selectFrom(Tables.TOKENREQUEST)
          .where(Tables.TOKENREQUEST.EMAIL.equal(email))
          .and(Tables.TOKENREQUEST.AUTHCODE.equal(authCode)).fetchAny();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    if (tokenRequestRecord != null) {
      logger.info("ok - activating " + email);
      returnValue = createBankAndDeleteTokenRequest(email, authCode, locationId);
    }
    return returnValue;
  }

  /**
   * Delete a bank from the database.
   *
   * @param email    the email of the bank to delete
   * @param authCode the api key
   * @return <CODE>204</CODE> on success
   * <CODE>401</CODE> if the credentials don't belong to this bank
   * <CODE>404</CODE> if no bank was found for this email address
   */
  protected Response.Status delete(String email, String authCode) {
    Response.Status responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
    BankDao bankDao;
    AuthtokenDao authtokenDao;
    Bank bank;
    Authtoken authToken;
    BankSite bankSite;
    BankSiteDao bankSiteDao;

    try (Connection connection = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(connection)
          .set(SQLDialect.POSTGRES);
      bankDao = new BankDao(configuration);
      bank = bankDao.fetchOneByEmail(email);

      if (bank != null) {
        authtokenDao = new AuthtokenDao(configuration);
        authToken = authtokenDao.fetchOneByValue(authCode);

        if (authToken == null) {
          logger.info("Invalid credentials provided by " + email);
          responseStatus = Response.Status.UNAUTHORIZED;
        } else if (!Objects.equals(authToken.getId(), bank.getAuthtokenId())) {
          logger.info("Invalid credentials provided by " + email);
          responseStatus = Response.Status.UNAUTHORIZED;
        } else { // everything correct
          logger.info("Deleting " + email);
          bankSiteDao = new BankSiteDao(configuration);
          List<BankSite> bankSiteList = bankSiteDao.fetchByBankId(bank.getId());
          if (bankSiteList.size() > 0) {
            bankSite = bankSiteList.get(0);
            bankSiteDao.delete(bankSite);
          }
          authtokenDao.delete(authToken);
          bankDao.delete(bank);

          responseStatus = Response.Status.NO_CONTENT;
        }
      } else { // bank == null
        logger.info("Invalid email address " + email);
        responseStatus = Response.Status.NOT_FOUND;
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    return responseStatus;
  }

  /**
   * Creates the bank and delete the token request.
   *
   * @param email    the email of the new bank
   * @param authCode the auth code
   * @return a string representation of the outcome
   */
  protected String createBankAndDeleteTokenRequest(String email, String authCode,
      String locationId) {
    String newAuthToken = SamplyShareUtils.createRandomAlphanumericString(64);
    String returnValue = newAuthToken;

    try (Connection connection = ResourceManager.getConnection()) {
      connection.setAutoCommit(false);
      DSLContext dslContext = ResourceManager.getDslContext(connection);

      Record record = dslContext.insertInto(Tables.AUTHTOKEN, Tables.AUTHTOKEN.VALUE)
          .values(newAuthToken).returning(Tables.AUTHTOKEN.ID).fetchOne();

      Bank bank = new Bank();
      bank.setEmail(email);
      bank.setAuthtokenId(record.getValue(Tables.AUTHTOKEN.ID));
      if (locationId != null) {
        Site site = SiteUtil.fetchSiteByName(locationId);
        BankSiteUtil.setSiteForBank(bank, site, true);
      }
      Configuration configuration = new DefaultConfiguration().set(connection)
          .set(SQLDialect.POSTGRES);
      BankDao bankDao = new BankDao(configuration);
      bankDao.insert(bank);

      TokenrequestDao tokenRequestDao = new TokenrequestDao(configuration);
      List<Tokenrequest> tokenRequestList = tokenRequestDao.fetchByEmail(email);

      for (Tokenrequest tokenRequest : tokenRequestList) {
        if (tokenRequest.getAuthcode().equals(authCode)) {
          tokenRequestDao.delete(tokenRequest);
          break;
        }
      }
      connection.commit();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
      returnValue = "error";
    }
    return returnValue;
  }

  /**
   * Check if a bank is registered with that email.
   *
   * @param email    the email
   * @param authCode the auth code
   * @return <CODE>200</CODE> if email and authcode are ok
   * <CODE>202</CODE> if registration is started but waiting for confirmation via token
   * <CODE>403</CODE> if email is registered but authcode is wrong (TODO maybe remove that?)
   * <CODE>404</CODE> if email is unknown
   */
  protected Response.Status checkStatus(String email, String authCode) {
    Bank bank;
    BankDao bankDao;
    Authtoken authToken;
    AuthtokenDao authTokenDao;
    TokenrequestDao tokenRequestDao;
    List<Tokenrequest> tokenRequestList;

    try (Connection connection = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(connection)
          .set(SQLDialect.POSTGRES);

      bankDao = new BankDao(configuration);
      bank = bankDao.fetchOneByEmail(email);

      // Bank with email is found
      if (bank != null) {
        authTokenDao = new AuthtokenDao(configuration);
        authToken = authTokenDao.fetchOneById(bank.getAuthtokenId());

        if (authToken != null) {
          if (authToken.getValue().equals(authCode)) {
            return Response.Status.OK;
          } else {
            return Response.Status.FORBIDDEN;
          }
        } else {
          return Response.Status.FORBIDDEN;
        }
      } else { // Bank is not yet registered. Check if tokenrequest is available
        tokenRequestDao = new TokenrequestDao(configuration);
        tokenRequestList = tokenRequestDao.fetchByEmail(email);
        if (tokenRequestList != null && tokenRequestList.size() > 0) {
          return Response.Status.ACCEPTED;
        } else {
          return Response.Status.NOT_FOUND;
        }
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }

    return Response.Status.INTERNAL_SERVER_ERROR;
  }
}
