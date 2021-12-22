package de.samply.share.broker.rest;

import de.samply.share.broker.utils.Utils;
import de.samply.share.common.utils.Constants;
import de.samply.share.common.utils.ProjectInfo;
import de.samply.share.model.common.And;
import de.samply.share.model.common.Attribute;
import de.samply.share.model.common.Contact;
import de.samply.share.model.common.Eq;
import de.samply.share.model.common.Info;
import de.samply.share.model.common.Inquiry;
import de.samply.share.model.common.ObjectFactory;
import de.samply.share.model.common.Or;
import de.samply.share.model.common.Query;
import de.samply.share.model.common.Where;
import de.samply.share.model.cql.CqlQuery;
import de.samply.share.model.cql.CqlQueryList;
import de.samply.share.utils.QueryConverter;
import java.io.IOException;
import java.io.StringWriter;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class Test provides a REST resources for samply.share.client (and other similar products) to
 * get and put example messages for testing purposes.
 */
@Path("/test")
public class Test {

  private static final Logger logger = LogManager.getLogger(Test.class);

  private static final String QUERYLANGUAGE_QUERY = "QUERY";
  private static final String QUERYLANGUAGE_CQL = "CQL";
  final String serverHeaderValue =
      Constants.SERVER_HEADER_VALUE_PREFIX + ProjectInfo.INSTANCE.getVersionString();

  @Context
  UriInfo uriInfo;

  /**
   * Construct an example query with a single criteria (gender = male).
   *
   * @return string representation of the example query
   */
  private static Query getExampleQuery() {
    ObjectFactory objectFactory = new ObjectFactory();
    Eq eq = new Eq();
    Or or = new Or();
    Attribute attribute = new Attribute();
    if (ProjectInfo.INSTANCE.getProjectName().equalsIgnoreCase("samply")) {
      attribute.setMdrKey("urn:mdr16:dataelement:23:1");
      attribute.setValue(objectFactory.createValue("female"));

    } else if (ProjectInfo.INSTANCE.getProjectName().equalsIgnoreCase("dktk")) {
      attribute.setMdrKey("urn:dktk:dataelement:1:*");
      attribute.setValue(objectFactory.createValue("M"));
    }
    eq.setAttribute(attribute);
    or.getAndOrEqOrLike().add(eq);
    And and = new And();
    and.getAndOrEqOrLike().add(or);
    Where where = new Where();
    where.getAndOrEqOrLike().add(and);
    Query query = new Query();
    query.setWhere(where);

    return query;
  }

  private static CqlQuery getExampleCqlQuery() {
    CqlQuery cqlQuery = new CqlQuery();
    cqlQuery.setCql("library Retrieve\n"
        + "using FHIR version '4.0.0'\n"
        + "include FHIRHelpers version '4.0.0'\n"
        + "context Patient\n"
        + "define InInitialPopulation:\n"
        + "    Patient.gender = 'male'");
    cqlQuery.setEntityType("Patient");
    return cqlQuery;
  }

  /**
   * Create a dummy contact.
   *
   * @return the created dummy contact
   */
  private static Contact getDummyContact() {
    Contact contact = new Contact();
    contact.setEmail("no-reply@ccp-it.dktk.dkfz.de");
    contact.setFirstname("n/a");
    contact.setLastname("n/a");
    contact.setOrganization("DKTK CCP-IT");
    contact.setPhone("-");
    contact.setTitle("-");
    return contact;
  }

  /**
   * Create a dummy info.
   *
   * @return the created dummy info
   */
  private static Info getDummyInfo() {
    Info info = new Info();
    info.setDescription("This is just a testquery.");
    info.setLabel("Testquery");
    info.setRevision("1");
    return info;
  }

  /**
   * Gets the example inquiry, no matter which id is provided.
   *
   * @return <CODE>200</CODE> and the serialized example inquiry on success
   *        <CODE>500</CODE> on any error
   */
  @Path("/inquiries/{inquiryid}")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response getInquiry(@HeaderParam(Constants.HEADER_XML_NAMESPACE) String xmlNamespaceHeader,
      @HeaderParam(Constants.QUERY_LANGUAGE) String queryLanguage) {
    try {
      Inquiry inquiry = new Inquiry();
      inquiry.setAuthor(getDummyContact());
      inquiry.setDescription(getDummyInfo().getDescription());

      UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
      inquiry.setExposeUrl(uriBuilder.path("test/exposes/0").build().toString());

      inquiry.setId("0");
      inquiry.setLabel(getDummyInfo().getLabel());
      if (queryLanguage.equals(QUERYLANGUAGE_QUERY)) {
        inquiry.setQuery(getExampleQuery());
      }
      if (queryLanguage.equals(QUERYLANGUAGE_CQL)) {
        CqlQueryList cqlQueryList = new CqlQueryList();
        cqlQueryList.getQueries().add(getExampleCqlQuery());
        inquiry.setCqlQueryList(cqlQueryList);
      }
      inquiry.setRevision("1");
      inquiry.getSearchFor().add("patienten");

      JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      StringWriter stringWriter = new StringWriter();
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

      ObjectFactory objectFactory = new ObjectFactory();

      marshaller.marshal(objectFactory.createInquiry(inquiry), stringWriter);

      String ret = stringWriter.toString();
      ret = Utils.fixNamespaces(ret, xmlNamespaceHeader);
      return Response.status(Status.OK).entity(ret)
          .header(Constants.SERVER_HEADER_KEY, serverHeaderValue).build();
    } catch (JAXBException e) {
      logger.error("Caught JAXB Exception while trying to provide example inquiry", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Gets the query part of the inquiry with the given id.
   *
   * @return <CODE>200</CODE> and the serialized query part of the example inquiry on success
   *        <CODE>500</CODE> on any error
   */
  @Path("/inquiries/{inquiryid}/query")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response getQuery(@HeaderParam(Constants.HEADER_XML_NAMESPACE) String xmlNamespaceHeader,
      @HeaderParam(Constants.QUERY_LANGUAGE) String queryLanguage) {
    try {
      String ret = "";
      if (queryLanguage.equals(QUERYLANGUAGE_CQL)) {
        CqlQuery cqlQuery = getExampleCqlQuery();
        ret = cqlQuery.getCql();
      } else if (queryLanguage.equals(QUERYLANGUAGE_QUERY)) {
        Query query = getExampleQuery();
        ret = QueryConverter.queryToXml(query);
        ret = Utils.fixNamespaces(ret, xmlNamespaceHeader);
      }
      return Response.status(Response.Status.OK).entity(ret)
          .header(Constants.SERVER_HEADER_KEY, serverHeaderValue).build();
    } catch (JAXBException e) {
      logger.error("Caught JAXB Exception while trying to provide example query", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Gets a dummy contact for the example inquiry.
   *
   * @return <CODE>200</CODE> and the serialized dummy contact on success
   *        <CODE>500</CODE> on any error
   */
  @Path("/inquiries/{inquiryid}/contact")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response getContact(
      @HeaderParam(Constants.HEADER_XML_NAMESPACE) String xmlNamespaceHeader) {
    Contact contact = getDummyContact();
    try {
      String ret;
      JAXBContext jaxbContext = JAXBContext.newInstance(Contact.class);
      ObjectFactory objectFactory = new ObjectFactory();
      StringWriter stringWriter = new StringWriter();
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
      marshaller.marshal(objectFactory.createContact(contact), stringWriter);

      ret = stringWriter.toString();
      ret = Utils.fixNamespaces(ret, xmlNamespaceHeader);

      return Response.status(Response.Status.OK).entity(ret)
          .header(Constants.SERVER_HEADER_KEY, serverHeaderValue).build();
    } catch (JAXBException e) {
      logger.error("Caught JAXB Exception while trying to provide example contact", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Gets a dummy description for the example inquiry.
   *
   * @return <CODE>200</CODE> and the serialized dummy info on success
   *        <CODE>500</CODE> on any error
   */
  @Path("/inquiries/{inquiryid}/info")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response getInfo(@HeaderParam(Constants.HEADER_XML_NAMESPACE) String xmlNamespaceHeader) {
    Info info = getDummyInfo();

    try {
      String ret;
      JAXBContext jaxbContext = JAXBContext.newInstance(Info.class);
      ObjectFactory objectFactory = new ObjectFactory();
      StringWriter stringWriter = new StringWriter();
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
      marshaller.marshal(objectFactory.createInfo(info), stringWriter);

      ret = stringWriter.toString();
      ret = Utils.fixNamespaces(ret, xmlNamespaceHeader);

      return Response.status(Response.Status.OK).entity(ret)
          .header(Constants.SERVER_HEADER_KEY, serverHeaderValue).build();
    } catch (JAXBException e) {
      logger.error("Caught JAXB Exception while trying to provide example info", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Always return that no expose is available here.
   *
   * @param inquiryId an irrelevant id that is accepted but ignored in order to fit the requests
   * @return <CODE>404</CODE> in any case
   */
  @Path("/inquiries/{inquiryid}/hasexpose")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response isSynopsisAvailable(@PathParam("inquiryid") String inquiryId) throws IOException {
    return Response.status(Response.Status.NOT_FOUND).entity("unavailable")
        .header(Constants.SERVER_HEADER_KEY, serverHeaderValue).build();
  }

  /**
   * Accept any kind of reply here.
   *
   * @param reply the reply
   * @return <CODE>200</CODE> in any case
   */
  @Path("/inquiries/{inquiryid}/replies/{bankemail}")
  @PUT
  @Consumes(MediaType.TEXT_PLAIN)
  public Response putReply(String reply) {
    return Response.status(Response.Status.OK)
        .header(Constants.SERVER_HEADER_KEY, serverHeaderValue).build();
  }
}
