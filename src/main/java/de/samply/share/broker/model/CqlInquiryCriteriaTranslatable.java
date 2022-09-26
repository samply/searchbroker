package de.samply.share.broker.model;

import de.samply.share.broker.model.db.enums.InquiryCriteriaType;
import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.InquiryCriteria;
import de.samply.share.model.cql.CqlQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CqlInquiryCriteriaTranslatable implements InquiryCriteriaTranslatable {

  private final List<CqlQuery> queries;

  public CqlInquiryCriteriaTranslatable(List<CqlQuery> queries) {
    this.queries = new ArrayList<>(Objects.requireNonNull(queries));
  }

  @Override
  public List<InquiryCriteria> createCriteria(Inquiry inquiry) {
    return queries.stream().map(query -> {
      InquiryCriteria inquiryCriteria = new InquiryCriteria();
      inquiryCriteria.setCriteria(query.getCql());
      inquiryCriteria.setInquiryId(inquiry.getId());
      inquiryCriteria.setType(InquiryCriteriaType.IC_CQL);
      inquiryCriteria.setEntityType(query.getEntityType());
      return inquiryCriteria;
    }).collect(Collectors.toList());
  }
}
