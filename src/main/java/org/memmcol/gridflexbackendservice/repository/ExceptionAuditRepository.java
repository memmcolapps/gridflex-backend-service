package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.ExceptionErrorLogs;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExceptionAuditRepository extends MongoRepository<ExceptionErrorLogs, String> {
}
