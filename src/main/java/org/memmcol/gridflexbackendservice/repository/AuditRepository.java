package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}