package de.samply.share.broker.utils.cql;

import de.samply.share.essentialquery.EssentialSimpleValueDto;
import de.samply.share.essentialquery.EssentialValueType;
import de.samply.share.query.enums.SimpleValueCondition;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class CqlExpressionFactory {

  private static final Logger logger = LogManager.getLogger(CqlExpressionFactory.class);

  private final MultiKeyMap<String, CqlConfig.CqlAtomicExpressionEntry> mapAtomicExpressions =
      new MultiKeyMap<>();
  private final MultiKeyMap<String, CqlConfig.CqlEntityTypeEntry> mapPathExpressions =
      new MultiKeyMap<>();
  private final MultiKeyMap<String, List<String>> mapPermittedValues = new MultiKeyMap<>();
  private final MultiKeyMap<String, Set<CqlConfig.Singleton>> mapSingletons = new MultiKeyMap<>();
  private final Map<String, Set<CqlConfig.Codesystem>> mapCodesystems = new HashMap<>();
  private final Map<String, String> mapExtensions = new HashMap<>();
  private String libraryTemplate = "";

  CqlExpressionFactory() {
    try (InputStream cqlConfigStream = CqlExpressionFactory.class
        .getResourceAsStream("samply_cql_config.xml")) {
      initMaps(cqlConfigStream);
    } catch (IOException e) {
      logger.warn("No valid config resource 'samply_cql_config.xml' could be found", e);
    }
  }

  private void initMaps(InputStream cqlConfigStream) {
    CqlConfig mapping;
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(CqlConfig.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      mapping = (CqlConfig) unmarshaller.unmarshal(cqlConfigStream);
    } catch (JAXBException e) {
      logger.warn("Config resource 'samply_cql_config.xml' could not be unmarshalled: ", e);
      return;
    }
    initMaps(mapping);
  }

  private void initMaps(CqlConfig mapping) {
    this.libraryTemplate = mapping.getTemplate();

    for (CqlConfig.CqlMdrFieldEntry mdrFieldEntry : mapping.getMdrFieldEntryList()) {
      for (CqlConfig.Codesystem codesystem : mdrFieldEntry.getCodesystemList()) {
        Set<CqlConfig.Codesystem> codesystems = mapCodesystems
            .getOrDefault(mdrFieldEntry.getMdrUrn(), new HashSet<>());

        codesystems.add(codesystem);
        mapCodesystems.put(mdrFieldEntry.getMdrUrn(), codesystems);
      }

      if (!StringUtils.isBlank(mdrFieldEntry.getExtensionUrl())) {
        mapExtensions.put(mdrFieldEntry.getMdrUrn(), mdrFieldEntry.getExtensionUrl());
      }
    }

    for (CqlConfig.CqlMdrFieldEntry mdrFieldEntry : mapping.getMdrFieldEntryList()) {
      for (CqlConfig.CqlEntityTypeEntry entityTypeEntry : mdrFieldEntry.getEntityTypeEntryList()) {
        for (CqlConfig.CqlAtomicExpressionEntry atomicExpressionEntry : entityTypeEntry
            .getAtomicExpressionList()) {
          mapAtomicExpressions.put(mdrFieldEntry.getMdrUrn(), entityTypeEntry.getEntityTypeName(),
              atomicExpressionEntry.getOperator(), atomicExpressionEntry);
        }
      }
    }

    for (CqlConfig.CqlMdrFieldEntry mdrFieldEntry : mapping.getMdrFieldEntryList()) {
      for (CqlConfig.CqlEntityTypeEntry entityTypeEntry : mdrFieldEntry.getEntityTypeEntryList()) {
        for (CqlConfig.Singleton singleton : entityTypeEntry.getSingletonList()) {
          Set<CqlConfig.Singleton> singletons = mapSingletons
              .get(mdrFieldEntry.getMdrUrn(), entityTypeEntry.getEntityTypeName());
          if (CollectionUtils.isEmpty(singletons)) {
            singletons = new HashSet<>();
          }
          singletons.add(singleton);

          mapSingletons
              .put(mdrFieldEntry.getMdrUrn(), entityTypeEntry.getEntityTypeName(), singletons);
        }
      }
    }

    for (CqlConfig.CqlMdrFieldEntry mdrFieldEntry : mapping.getMdrFieldEntryList()) {
      for (CqlConfig.CqlEntityTypeEntry entityTypeEntry : mdrFieldEntry.getEntityTypeEntryList()) {
        mapPathExpressions
            .put(mdrFieldEntry.getMdrUrn(), entityTypeEntry.getEntityTypeName(), entityTypeEntry);
      }
    }

    for (CqlConfig.CqlMdrFieldEntry mdrFieldEntry : mapping.getMdrFieldEntryList()) {
      for (CqlConfig.PermittedValueEntry permittedValueEntry : mdrFieldEntry
          .getPermittedValueEntryList()) {
        List<String> cqlValues = mapPermittedValues
            .get(mdrFieldEntry.getMdrUrn(), permittedValueEntry.getMdrKey());
        if (cqlValues == null) {
          cqlValues = new ArrayList<>();
        }
        cqlValues.add(permittedValueEntry.getCqlValue());

        mapPermittedValues
            .put(mdrFieldEntry.getMdrUrn(), permittedValueEntry.getMdrKey(), cqlValues);
      }
    }
  }

  String getAtomicExpression(
      CqlExpressionFactory.AtomicExpressionParameter atomicExpressionParameter) {
    String mdrUrn = atomicExpressionParameter.getMdrUrn();
    String entityType = atomicExpressionParameter.getEntityType();
    String operator = atomicExpressionParameter.getOperator();

    CqlConfig.CqlAtomicExpressionEntry cqlAtomicExpressionEntry = mapAtomicExpressions
        .get(mdrUrn, entityType, operator);
    if (cqlAtomicExpressionEntry == null) {
      cqlAtomicExpressionEntry = mapAtomicExpressions.get(mdrUrn, entityType, "DEFAULT");
      if (cqlAtomicExpressionEntry == null) {
        logger.warn(
            "No valid cql configuration found for entity type '" + entityType + "' and mdrUrn '"
                + mdrUrn + "' and operator '" + operator + "'");
        return "";
      }
    }

    //noinspection ConfusingArgumentToVarargsMethod
    return MessageFormat.format(cqlAtomicExpressionEntry.getAtomicCqlExpression(),
        atomicExpressionParameter.asVarArgParameter());
  }

  String getPathExpression(String mdrUrn, String entityType, String valuesExpression) {
    CqlConfig.CqlEntityTypeEntry cqlEntityTypeEntry1 = mapPathExpressions.get(mdrUrn, entityType);
    if (cqlEntityTypeEntry1 == null) {
      logger.warn(
          "No valid cql configuration found for entity type '" + entityType + "' and mdrUrn '"
              + mdrUrn + "'");
      return "";
    }

    return MessageFormat.format(cqlEntityTypeEntry1.getPathCqlExpression(), valuesExpression);
  }

  String createLibrary(String entityType, String codesystems, String singletons, String predicate,
      String stratifierStatements) {
    return MessageFormat.format(libraryTemplate, entityType, codesystems, singletons, predicate,
        stratifierStatements);
  }

  String getExtensionUrl(String mdrUrn) {
    return mapExtensions.getOrDefault(mdrUrn, "");
  }

  Set<CqlConfig.Codesystem> getCodesystems(String mdrUrn) {
    return mapCodesystems.getOrDefault(mdrUrn, new HashSet<>());
  }

  List<AtomicExpressionParameter> createAtomicExpressionParameterList(String mdrUrn,
      String entityType, EssentialValueType valueType, EssentialSimpleValueDto valueDto) {
    List<AtomicExpressionParameter> atomicExpressionParameters = new ArrayList<>();

    List<String> cqlValues = getCqlValueList(mdrUrn, valueType.asCqlString(valueDto.getValue()));
    if (cqlValues.size() == 1) {
      return Collections.singletonList(
          new AtomicExpressionParameter(mdrUrn, entityType, valueDto.getCondition(),
              cqlValues.get(0), valueType.asCqlString(valueDto.getMaxValue())));
    }

    for (String cqlValue : cqlValues) {
      // We have more than one cqlValue only when we deal with PermittedValues in which case we do
      // not have a maximal value
      String cqlMaxValue = "";

      atomicExpressionParameters.add(
          new AtomicExpressionParameter(mdrUrn, entityType, valueDto.getCondition(), cqlValue,
              cqlMaxValue));
    }

    return atomicExpressionParameters;
  }

  List<String> getCqlValueList(String mdrUrn, String mdrValue) {
    List<String> cqlValues = mapPermittedValues.get(mdrUrn, mdrValue);
    if (CollectionUtils.isEmpty(cqlValues)) {
      return new ArrayList<>(Collections.singletonList(mdrValue));
    }

    return cqlValues;
  }

  Set<CqlConfig.Singleton> getSingletons(String mdrUrn, String entityType) {
    Set<CqlConfig.Singleton> singletons = mapSingletons.get(mdrUrn, entityType);

    return !CollectionUtils.isEmpty(singletons) ? singletons : new HashSet<>();
  }

  class AtomicExpressionParameter {

    private final String mdrUrn;
    private final String entityType;
    private final SimpleValueCondition condition;
    private final String value;
    private final String maxValue;

    private final String extensionUrl;

    AtomicExpressionParameter(String mdrUrn, String entityType, SimpleValueCondition condition,
        String value, String maxValue) {
      this.mdrUrn = mdrUrn;
      this.entityType = entityType;
      this.condition = condition;
      this.value = value;
      this.maxValue = maxValue;
      this.extensionUrl = getExtensionUrl(mdrUrn);
    }

    String getMdrUrn() {
      return mdrUrn;
    }

    String getEntityType() {
      return entityType;
    }

    String getOperator() {
      switch (condition) {
        case BETWEEN:
          return "...";
        case EQUALS:
          return "=";
        case GREATER:
          return ">";
        case LESS:
          return "<";
        case NOT_EQUALS:
          return "<>";
        case LESS_OR_EQUALS:
          return "<=";
        case GREATER_OR_EQUALS:
          return ">=";
        default:
          return "default";
      }
    }

    String[] asVarArgParameter() {
      return new String[]{getOperator(), extensionUrl, value, maxValue};
    }
  }
}
