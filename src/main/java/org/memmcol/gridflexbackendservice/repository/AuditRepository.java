package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findAllByCreator_OrgId(UUID orgId);
    Page<AuditLog> findAllByCreator_OrgIdOrderByCreatedAtDesc(UUID orgId,Pageable pageable);
    Optional<AuditLog> findByIdAndCreator_OrgId(String id, UUID orgId);
}
