package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.OperatorAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<OperatorAudit, String> {
}