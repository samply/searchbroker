package de.samply.share.broker.model;

import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.InquiryCriteria;
import java.util.List;

public interface InquiryCriteriaTranslatable {

  List<InquiryCriteria> createCriteria(Inquiry inquiry);
}
