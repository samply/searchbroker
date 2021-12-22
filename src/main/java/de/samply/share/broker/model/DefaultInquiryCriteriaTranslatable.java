package de.samply.share.broker.model;

import static de.samply.share.broker.model.db.enums.InquiryCriteriaType.IC_CQL;
import static de.samply.share.broker.model.db.enums.InquiryCriteriaType.IC_QUERY;
import static java.lang.Boolean.TRUE;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;

import com.google.gson.Gson;
import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.InquiryCriteria;
import de.samply.share.broker.statistics.StatisticsHandler;
import de.samply.share.broker.utils.EssentialSimpleQueryDto2ShareXmlTransformer;
import de.samply.share.broker.utils.cql.EssentialSimpleQueryDto2CqlTransformer;
import de.samply.share.essentialquery.EssentialSimpleFieldDto;
import de.samply.share.essentialquery.EssentialSimpleQueryDto;
import de.samply.share.essentialquery.EssentialSimpleValueDto;
import de.samply.share.model.common.And;
import de.samply.share.model.common.ObjectFactory;
import de.samply.share.model.common.Query;
import de.samply.share.model.common.Where;
import de.samply.share.query.enums.SimpleValueCondition;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultInquiryCriteriaTranslatable implements
    InquiryCriteriaTranslatable {

  private static final Logger logger =
      LogManager.getLogger(DefaultInquiryCriteriaTranslatable.class);

  private static final String ENTITY_TYPE_FOR_QUERY = "Donor + Sample";
  private static final String ENTITY_TYPE_FOR_CQL_PATIENT = "Patient";
  private static final String ENTITY_TYPE_FOR_CQL_SPECIMEN = "Specimen";

  private final String query;

  public DefaultInquiryCriteriaTranslatable(String query) {
    this.query = Objects.requireNonNull(query);
  }

  /**
   * Creates an inquiry criteria from {@code query}.
   *
   * @param query   the query to translate
   * @param inquiry the inquiry the criteria should refer to
   * @return {@link Optional#empty()} if there is a marshalling problem
   */
  public static Optional<InquiryCriteria> createInquiryCriteriaTypeQuery(Query query,
      Inquiry inquiry) {
    return marshallQuery(query).map(criteria -> {
      InquiryCriteria inquiryCriteria = new InquiryCriteria();
      inquiryCriteria.setCriteria(criteria);
      inquiryCriteria.setInquiryId(inquiry.getId());
      inquiryCriteria.setType(IC_QUERY);
      inquiryCriteria.setEntityType(ENTITY_TYPE_FOR_QUERY);
      return inquiryCriteria;
    });
  }

  private static Optional<String> marshallQuery(Query query) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
      StringWriter stringWriter = new StringWriter();
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(JAXB_FRAGMENT, TRUE);
      marshaller.marshal(query, stringWriter);
      return Optional.of(stringWriter.toString());
    } catch (JAXBException e) {
      logger.error("Error while marshalling a query.");
      return Optional.empty();
    }
  }

  @Override
  public List<InquiryCriteria> createCriteria(Inquiry inquiry) {
    EssentialSimpleQueryDto essentialSimpleQueryDto = jsonString2EssentialDto(query);
    createAndSaveStatistics(essentialSimpleQueryDto, inquiry.getId());
    return createInquiryCriteria(essentialSimpleQueryDto, inquiry);
  }

  private EssentialSimpleQueryDto jsonString2EssentialDto(String simpleQueryDtoJson) {
    EssentialSimpleQueryDto queryDto = new EssentialSimpleQueryDto();

    try {
      Gson gson = new Gson();
      queryDto = gson.fromJson(simpleQueryDtoJson, EssentialSimpleQueryDto.class);
    } catch (Exception e) {
      logger.error(e);
      e.printStackTrace();
    }

    for (EssentialSimpleFieldDto field : queryDto.getFieldDtos()) {
      field.setValueDtos(
          field.getValueDtos().stream()
              .filter(valueDto -> !isEmpty(valueDto))
              .collect(Collectors.toList()));
    }

    queryDto.setFieldDtos(queryDto.getFieldDtos().stream()
        .filter(fieldDto -> !isEmpty(fieldDto))
        .collect(Collectors.toList()));

    return queryDto;
  }

  private boolean isEmpty(EssentialSimpleValueDto valueDto) {
    return isEmptyValue(valueDto.getValue())
        || (valueDto.getCondition() == SimpleValueCondition.BETWEEN && isEmptyValue(
        valueDto.getMaxValue()));
  }

  private boolean isEmpty(EssentialSimpleFieldDto fieldDto) {
    return CollectionUtils.isEmpty(fieldDto.getValueDtos());
  }

  private boolean isEmptyValue(String value) {
    return StringUtils.isEmpty(value) || "null".equalsIgnoreCase(value);
  }

  private void createAndSaveStatistics(EssentialSimpleQueryDto essentialSimpleQueryDto,
      Integer inquiryId) {
    StatisticsHandler statisticsHandler = new StatisticsHandler();
    statisticsHandler.save(essentialSimpleQueryDto, inquiryId);
  }

  private List<InquiryCriteria> createInquiryCriteria(
      EssentialSimpleQueryDto essentialSimpleQueryDto, Inquiry inquiry) {
    List<InquiryCriteria> inquiryCriteria = new ArrayList<>(this.createInquiryCriteriaTypeCql(
        essentialSimpleQueryDto, inquiry));

    createInquiryCriteriaTypeEssentialSimpleQueryDto(essentialSimpleQueryDto, inquiry)
        .ifPresent(inquiryCriteria::add);

    return inquiryCriteria;
  }

  private List<InquiryCriteria> createInquiryCriteriaTypeCql(
      EssentialSimpleQueryDto essentialSimpleQueryDto, Inquiry inquiry) {
    List<InquiryCriteria> inquiryCriteria = new ArrayList<>();
    inquiryCriteria.add(createInquiryCriteriaTypeCqlPatient(essentialSimpleQueryDto, inquiry));
    inquiryCriteria.add(createInquiryCriteriaTypeCqlSpecimen(essentialSimpleQueryDto, inquiry));
    return inquiryCriteria;
  }

  private InquiryCriteria createInquiryCriteriaTypeCql(String cql, Inquiry inquiry,
      String entityType) {
    InquiryCriteria inquiryCriteria = new InquiryCriteria();
    inquiryCriteria.setCriteria(cql);
    inquiryCriteria.setInquiryId(inquiry.getId());
    inquiryCriteria.setType(IC_CQL);
    inquiryCriteria.setEntityType(entityType);
    return inquiryCriteria;
  }

  private InquiryCriteria createInquiryCriteriaTypeCqlPatient(
      EssentialSimpleQueryDto essentialSimpleQueryDto, Inquiry inquiry) {
    String cql = createCqlPatient(essentialSimpleQueryDto);

    return createInquiryCriteriaTypeCql(cql, inquiry, ENTITY_TYPE_FOR_CQL_PATIENT);
  }

  private InquiryCriteria createInquiryCriteriaTypeCqlSpecimen(
      EssentialSimpleQueryDto essentialSimpleQueryDto, Inquiry inquiry) {
    String cql = createCqlSpecimen(essentialSimpleQueryDto);

    return createInquiryCriteriaTypeCql(cql, inquiry, ENTITY_TYPE_FOR_CQL_SPECIMEN);
  }

  private String createCqlPatient(EssentialSimpleQueryDto essentialSimpleQueryDto) {
    return createCql(essentialSimpleQueryDto, ENTITY_TYPE_FOR_CQL_PATIENT);
  }

  private String createCqlSpecimen(EssentialSimpleQueryDto essentialSimpleQueryDto) {
    return createCql(essentialSimpleQueryDto, ENTITY_TYPE_FOR_CQL_SPECIMEN);
  }

  private String createCql(EssentialSimpleQueryDto essentialSimpleQueryDto, String entityType) {
    return new EssentialSimpleQueryDto2CqlTransformer()
        .toQuery(essentialSimpleQueryDto, entityType);
  }

  private Optional<InquiryCriteria> createInquiryCriteriaTypeEssentialSimpleQueryDto(
      EssentialSimpleQueryDto essentialSimpleQueryDto, Inquiry inquiry) {
    Query query;

    if (CollectionUtils.isEmpty(essentialSimpleQueryDto.getFieldDtos())) {
      query = new EssentialSimpleQueryDto2ShareXmlTransformer().toQuery(essentialSimpleQueryDto);
    } else {
      query = new Query();
      Where where = new Where();
      And and = new And();
      where.getAndOrEqOrLike().add(and);
      query.setWhere(where);
    }

    return createInquiryCriteriaTypeQuery(query, inquiry);
  }
}
