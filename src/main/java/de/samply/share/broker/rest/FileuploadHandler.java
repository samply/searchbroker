package de.samply.share.broker.rest;

import de.samply.share.broker.model.db.enums.DocumentType;
import de.samply.share.broker.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Handle the upload of any documents.
 */
@Path("/documentUpload")
public class FileuploadHandler {

  private static final Logger logger = LogManager.getLogger(FileuploadHandler.class);

  private static final String DOC_TYPE = "documentType";
  private static final String DOC_ID = "documentId";
  private static final String ERROR = "error";

  /**
   * Accept a file upload from the webinterface.
   *
   * @param userId                   the id of the user that uploads the file
   * @param docType                  the type of the document (expose, vote...)
   * @param fileInputStream          the file itself
   * @param contentDispositionHeader the content information
   * @return <CODE>200</CODE> on success
   * <CODE>500</CODE> on any error
   */
  @POST
  @Path("/user/{userid}/{doctype}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadFile(@PathParam("userid") Integer userId,
      @PathParam("doctype") String docType,
      @FormDataParam("documentUpload") InputStream fileInputStream,
      @FormDataParam("documentUpload") FormDataContentDisposition contentDispositionHeader)
      throws UnsupportedEncodingException, JSONException {
    JSONObject jsonObject = new JSONObject();
    int documentId;
    byte[] buf;

    try {
      buf = IOUtils.toByteArray(fileInputStream);
      logger.debug(buf.length);
    } catch (IOException e1) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    if (contentDispositionHeader == null) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    DocumentType documentType = DocumentType.DT_OTHER;
    if (docType.equalsIgnoreCase("expose")) {
      documentType = DocumentType.DT_EXPOSE;
    } else if (docType.equalsIgnoreCase("vote")) {
      documentType = DocumentType.DT_VOTE;
    }

    String filename = contentDispositionHeader.getFileName();
    String name = contentDispositionHeader.getName();
    String filetype = contentDispositionHeader.getType();

    logger.debug("filename: " + filename);
    logger.debug("name: " + name);
    logger.debug("file type: " + filetype);
    logger.debug("document type: " + documentType);

    try {
      File documentFile = Utils.saveByteArrayToTmpFile("document", buf, contentDispositionHeader);
      InquiryHandler inquiryHandler = new InquiryHandler();
      documentId = inquiryHandler
          .addDocument(null, null, userId, documentFile, filename, filetype, documentType);
      documentFile.delete();
    } catch (IOException e) {
      logger.error("Could not save uploaded file.", e);
      jsonObject.put(ERROR, "IOException");
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(jsonObject.toString()).build();
    } catch (NullPointerException npe) {
      logger.error("Null pointer while trying to store file....", npe);
      jsonObject.put(ERROR, "Null Pointer Exception");
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(jsonObject.toString()).build();
    }
    jsonObject.put(DOC_TYPE, docType);
    jsonObject.put(DOC_ID, documentId);

    logger.debug(jsonObject);
    return Response.ok(jsonObject.toString()).build();
  }

}
