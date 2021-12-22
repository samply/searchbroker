package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.enums.DocumentType;
import de.samply.share.broker.model.db.tables.daos.DocumentDao;
import de.samply.share.broker.model.db.tables.pojos.Document;
import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for CRUD operations for Document Objects.
 *
 * @see Document
 */
public final class DocumentUtil {

  private static final Logger logger = LoggerFactory.getLogger(DocumentUtil.class);

  // Prevent instantiation
  private DocumentUtil() {
  }

  /**
   * Get a document by its database id.
   *
   * @param documentId the id of the document
   * @return the document
   */
  public static Document getDocumentById(int documentId) {
    Document document = null;
    DocumentDao documentDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      documentDao = new DocumentDao(configuration);
      document = documentDao.fetchOneById(documentId);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return document;
  }

  /**
   * Get all documents linked with a project.
   *
   * @param projectId the id of the project
   * @return a list of documents linked with the project
   */
  public static List<Document> getDocumentsForProject(int projectId) {
    List<Document> documents = null;
    DocumentDao documentDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      documentDao = new DocumentDao(configuration);
      documents = documentDao.fetchByProjectId(projectId);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return documents;
  }

  /**
   * Get all documents of a certain type linked with a project.
   *
   * @param projectId    the id of the project
   * @param documentType the type of document to get
   * @return a list of documents linked with the project
   */
  public static List<Document> getDocumentsForProject(int projectId, DocumentType documentType) {
    List<Document> documents = null;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      documents = dslContext.select()
          .from(Tables.DOCUMENT)
          .where((Tables.DOCUMENT.DOCUMENT_TYPE.equal(documentType))
              .and(Tables.DOCUMENT.PROJECT_ID.equal(projectId))
          ).fetchInto(Document.class);

    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return documents;
  }

  /**
   * Get the final report for a project.
   *
   * @param projectId the id of the project
   * @return the final report or null if not available
   */
  public static Document fetchFinalReportByProjectId(int projectId) {
    List<Document> documents = getDocumentsForProject(projectId, DocumentType.DT_REPORT_FINAL);
    if (documents != null && documents.size() > 0) {
      // There should be only one final report. Return the first one.
      return documents.get(0);
    }
    return null;
  }

  /**
   * Update a document.
   *
   * @param document the document to update
   */
  public static void updateDocument(Document document) {
    DocumentDao documentDao;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      documentDao = new DocumentDao(configuration);
      documentDao.update(document);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
  }

  /**
   * Link a document with an inquiry.
   *
   * @param documentId the id of the document
   * @param inquiryId  the id of the inquiry to link the document with
   */
  public static void setInquiryIdForDocument(int documentId, Integer inquiryId) {
    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);

      dslContext.update(Tables.DOCUMENT)
          .set(Tables.DOCUMENT.INQUIRY_ID, inquiryId)
          .where(Tables.DOCUMENT.ID.equal(documentId))
          .execute();

    } catch (SQLException e) {
      logger.error("Error setting inquiry id " + inquiryId + " for document " + documentId);
    }
  }

  /**
   * Link a document, that is already linked to an inquiry, to the project of the inquiry as well.
   *
   * @param inquiryId the id of the inquiry, the document is currently linked with
   * @param projectId the id of the project to link the document with
   */
  public static void setProjectIdForDocumentByInquiryId(int inquiryId, int projectId) {
    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);

      dslContext.update(Tables.DOCUMENT)
          .set(Tables.DOCUMENT.PROJECT_ID, projectId)
          .where(Tables.DOCUMENT.INQUIRY_ID.equal(inquiryId))
          .execute();

    } catch (SQLException e) {
      logger.error(
          "Error setting project id " + projectId + " for document with inquiry id " + inquiryId);
    }
  }

  /**
   * Delete documents that are older than 2 days and are not referenced in any inquiry or project.
   *
   * @return the amount of deleted documents
   */
  public static int deleteOldUnboundDocuments() {
    int affectedRows = 0;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext create = ResourceManager.getDslContext(conn);

      affectedRows = create.delete(Tables.DOCUMENT)
          .where(DSL.dateDiff(DSL.currentDate(), Tables.DOCUMENT.UPLOADED_AT).greaterThan(2))
          .and(Tables.DOCUMENT.PROJECT_ID.isNull()
              .and(Tables.DOCUMENT.INQUIRY_ID.isNull())
          )
          .execute();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    logger.info("Deleted " + affectedRows + " old unbound documents");
    return affectedRows;
  }

  /**
   * Read a document into a bytestream.
   *
   * @param documentId the id of the document to read
   * @return the byte array stream of the document
   */
  public static ByteArrayOutputStream getDocumentOutputStreamById(int documentId) {
    Document document;
    DocumentDao documentDao;
    ByteArrayOutputStream bos = null;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      documentDao = new DocumentDao(configuration);
      document = documentDao.fetchOneById(documentId);
      bos = new ByteArrayOutputStream();
      bos.write(document.getData());
      bos.close();
    } catch (SQLException | IOException e) {
      logger.error(e.getMessage(),e);
    }
    return bos;
  }

  /**
   * Read an expose into a bytestream.
   *
   * @param inquiryId the id of the inquiry to which the expose belongs to
   * @return the byte array stream of the expose
   */
  public static ByteArrayOutputStream getExposeOutputStreamByInquiryId(int inquiryId) {
    return getDocumentOutputStreamById(getExposeIdByInquiryId(inquiryId));
  }

  /**
   * Delete a document from the database.
   *
   * @param documentId the id of the document to delete
   * @return the type of the deleted document
   */
  public static DocumentType deleteDocument(int documentId) {
    DocumentDao documentDao;
    Document document;
    DocumentType documentType = DocumentType.DT_OTHER;

    try (Connection conn = ResourceManager.getConnection()) {
      Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
      documentDao = new DocumentDao(configuration);
      document = documentDao.fetchOneById(documentId);
      documentType = document.getDocumentType();
      documentDao.delete(document);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return documentType;
  }

  /**
   * Delete an expose for an inquiry.
   *
   * @param inquiryId the id of the inquiry whose expose will be deleted
   */
  public static void deleteExposeByInquiryId(int inquiryId) {
    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);

      dslContext.deleteFrom(Tables.DOCUMENT)
          .where(Tables.DOCUMENT.DOCUMENT_TYPE.equal(DocumentType.DT_EXPOSE)
              .and(Tables.DOCUMENT.INQUIRY_ID.equal(inquiryId))
          )
          .execute();
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
  }

  /**
   * Get the id of the expose belonging to the given inquiry.
   *
   * @param inquiryId the id of the inquiry
   * @return the id of the expose, belonging to that inquiry
   */
  public static Integer getExposeIdByInquiryId(int inquiryId) {
    Document expose;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      expose = dslContext.select()
          .from(Tables.DOCUMENT)
          .where(Tables.DOCUMENT.DOCUMENT_TYPE.equal(DocumentType.DT_EXPOSE)
              .and(Tables.DOCUMENT.INQUIRY_ID.equal(inquiryId))
          )
          .fetchOneInto(Document.class);
      if (expose != null) {
        return expose.getId();
      } else {
        return null;
      }
    } catch (SQLException e) {
      logger
          .error("Exception caught while trying to get expose id for inquiry with id " + inquiryId,
              e);
    }
    return 0;
  }

  /**
   * Get the expose belonging to the given inquiry.
   *
   * @param inquiryId the id of the inquiry
   * @return the expose, belonging to that inquiry
   */
  public static Document fetchExposeByInquiryId(int inquiryId) {
    Document expose = null;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      expose = dslContext.select()
          .from(Tables.DOCUMENT)
          .where(Tables.DOCUMENT.DOCUMENT_TYPE.equal(DocumentType.DT_EXPOSE)
              .and(Tables.DOCUMENT.INQUIRY_ID.equal(inquiryId))
          )
          .fetchOneInto(Document.class);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return expose;
  }

  /**
   * Get the expose belonging to the given inquiry.
   *
   * @param inquiry the inquiry
   * @return the expose, belonging to that inquiry
   */
  public static Document fetchExposeByInquiry(Inquiry inquiry) {
    Document expose = null;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      expose = dslContext.select()
          .from(Tables.DOCUMENT)
          .where(Tables.DOCUMENT.DOCUMENT_TYPE.equal(DocumentType.DT_EXPOSE)
              .and(Tables.DOCUMENT.INQUIRY_ID.equal(inquiry.getId()))
          )
          .fetchOneInto(Document.class);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return expose;
  }

  /**
   * Get the vote belonging to the given inquiry.
   *
   * @param inquiry the inquiry
   * @return the vote, belonging to that inquiry
   */
  public static Document fetchVoteByInquiry(Inquiry inquiry) {
    Document vote = null;

    try (Connection conn = ResourceManager.getConnection()) {
      DSLContext dslContext = ResourceManager.getDslContext(conn);
      vote = dslContext.select()
          .from(Tables.DOCUMENT)
          .where(Tables.DOCUMENT.DOCUMENT_TYPE.equal(DocumentType.DT_VOTE)
              .and(Tables.DOCUMENT.INQUIRY_ID.equal(inquiry.getId()))
          )
          .fetchOneInto(Document.class);
    } catch (SQLException e) {
      logger.error(e.getMessage(),e);
    }
    return vote;
  }

  /**
   * Change the content of an expose file in the database.
   *
   * @param exposeId the id of the expose that will be changed
   * @param bos      the content of the new expose as byte array stream
   */
  public static void changeExposeData(int exposeId, ByteArrayOutputStream bos) throws IOException {
    Document expose;
    DocumentDao exposeDao;

    File newExposeFile = Files.createTempFile("changeexpose_", Integer.toString(exposeId)).toFile();
    FileOutputStream fos = new FileOutputStream(newExposeFile);
    fos.write(bos.toByteArray());
    fos.flush();
    fos.close();

    try (Connection connection = ResourceManager.getConnection()) {
      if (newExposeFile != null) {
        Path path = newExposeFile.toPath();
        Configuration configuration = new DefaultConfiguration().set(connection)
            .set(SQLDialect.POSTGRES);
        exposeDao = new DocumentDao(configuration);
        expose = exposeDao.fetchOneById(exposeId);
        expose.setData(Files.readAllBytes(path));
        exposeDao.update(expose);
        newExposeFile.delete();
      }
    } catch (SQLException | IOException e) {
      logger.error(e.getMessage(),e);
    }
  }
}
