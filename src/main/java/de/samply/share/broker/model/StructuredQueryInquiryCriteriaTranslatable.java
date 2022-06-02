package de.samply.share.broker.model;

import static de.samply.share.broker.model.db.enums.InquiryCriteriaType.IC_STRUCTURED_QUERY;

import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.InquiryCriteria;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StructuredQueryInquiryCriteriaTranslatable implements
    InquiryCriteriaTranslatable {

  private final String query;

  public StructuredQueryInquiryCriteriaTranslatable(String query) {
    this.query = Objects.requireNonNull(query);
  }

  @Override
  public List<InquiryCriteria> createCriteria(Inquiry inquiry) {
    InquiryCriteria inquiryCriteria = new InquiryCriteria();
    inquiryCriteria.setCriteria(query);
    inquiryCriteria.setInquiryId(inquiry.getId());
    inquiryCriteria.setType(IC_STRUCTURED_QUERY);
    inquiryCriteria.setEntityType("Patient");
    return Collections.singletonList(inquiryCriteria);
  }
}
