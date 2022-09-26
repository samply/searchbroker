package de.samply.share.broker.utils.cql;

import de.samply.share.essentialquery.EssentialSimpleQueryDto;

public class EssentialSimpleQueryDto2CqlTransformer {

  private static final CqlConfig.Codesystem SAMPLE_MATERIAL_TYPE = new CqlConfig.Codesystem(
      "SampleMaterialType",
      "https://fhir.bbmri.de/CodeSystem/SampleMaterialType");

  private final CqlExpressionFactory cqlExpressionFactory;

  public EssentialSimpleQueryDto2CqlTransformer() {
    this.cqlExpressionFactory = new CqlExpressionFactory();
  }

  /**
   * Convert the EssentialSimpleQueryDto to a CQL query.
   * @param essentialSimpleQueryDto essentialSimpleQueryDto with the search criteria.
   * @param entityType entityType (Patient or Specimen)
   * @return CQL query as String
   */
  public String toQuery(EssentialSimpleQueryDto essentialSimpleQueryDto, String entityType) {
    CqlCodeSystemDefinitionsFactory codeSystemDefinitionsFactory = new
        CqlCodeSystemDefinitionsFactory(cqlExpressionFactory);
    CqlSingletonStatementsFactory singletonsFactory = new CqlSingletonStatementsFactory(
        cqlExpressionFactory);
    CqlFieldExpressionFactory fieldExpressionFactory = new CqlFieldExpressionFactory(
        cqlExpressionFactory);
    CqlPredicateFactory predicateFactory = new CqlPredicateFactory(fieldExpressionFactory);

    String codeSystemDefinitions = codeSystemDefinitionsFactory
        .create(essentialSimpleQueryDto, SAMPLE_MATERIAL_TYPE);
    String singletonStatements = singletonsFactory.create(essentialSimpleQueryDto, entityType);
    String predicate = predicateFactory.create(essentialSimpleQueryDto, entityType);

    return cqlExpressionFactory
        .createLibrary(entityType, codeSystemDefinitions, singletonStatements,
            predicate, createStratifierStatements(entityType));
  }

  private String createStratifierStatements(String entityType) {
    switch (entityType) {
      case "Patient":
        return "define Gender:\n"
            + "  Patient.gender\n"
            + "\n"
            + "define AgeClass:\n"
            + "  (AgeInYears() div 10) * 10";
      case "Specimen":
        return "define TypeCodes:\n"
            + "  from Specimen.type.coding C return FHIRHelpers.ToCode(C)\n"
            + "\n"
            + "define SampleMaterialTypeCategory:\n"
            + "  case\n"
            + "    when\n"
            + "      exists (TypeCodes intersect {\n"
            + "        Code 'whole-blood' from SampleMaterialType,\n"
            + "        Code 'bone-marrow' from SampleMaterialType,\n"
            + "        Code 'buffy-coat' from SampleMaterialType,\n"
            + "        Code 'peripheral-blood-cells-vital' from SampleMaterialType,\n"
            + "        Code 'blood-plasma' from SampleMaterialType,\n"
            + "        Code 'plasma-edta' from SampleMaterialType,\n"
            + "        Code 'plasma-citrat' from SampleMaterialType,\n"
            + "        Code 'plasma-heparin' from SampleMaterialType,\n"
            + "        Code 'plasma-cell-free' from SampleMaterialType,\n"
            + "        Code 'plasma-other' from SampleMaterialType,\n"
            + "        Code 'blood-serum' from SampleMaterialType,\n"
            + "        Code 'ascites' from SampleMaterialType,\n"
            + "        Code 'csf-liquor' from SampleMaterialType,\n"
            + "        Code 'urine' from SampleMaterialType,\n"
            + "        Code 'liquid-other' from SampleMaterialType\n"
            + "      })\n"
            + "    then 'liquid'\n"
            + "    when\n"
            + "      exists (TypeCodes intersect {\n"
            + "        Code 'tissue-ffpe' from SampleMaterialType,\n"
            + "        Code 'tumor-tissue-ffpe' from SampleMaterialType,\n"
            + "        Code 'normal-tissue-ffpe' from SampleMaterialType,\n"
            + "        Code 'other-tissue-ffpe' from SampleMaterialType,\n"
            + "        Code 'tissue-frozen' from SampleMaterialType,\n"
            + "        Code 'tumor-tissue-frozen' from SampleMaterialType,\n"
            + "        Code 'normal-tissue-frozen' from SampleMaterialType,\n"
            + "        Code 'other-tissue-frozen' from SampleMaterialType,\n"
            + "        Code 'tissue-other' from SampleMaterialType\n"
            + "      })\n"
            + "    then 'tissue'\n"
            + "    else 'other'\n"
            + "  end"
            + "\n"
            + "define Custodian:\n"
            + "First(from Specimen.extension E\n"
            +  "where E.url = 'https://fhir.bbmri.de/StructureDefinition/Custodian'\n"
            +     "return (E.value as Reference).identifier.value)";
      default:
        return "";
    }
  }
}
