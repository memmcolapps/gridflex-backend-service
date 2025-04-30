package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<AuditLog, String> {
}