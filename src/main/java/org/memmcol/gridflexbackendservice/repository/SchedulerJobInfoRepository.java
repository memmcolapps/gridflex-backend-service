//package org.memmcol.gridflexbackendservice.repository;
//
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.SchedulerJobInfo;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@Repository
//public interface SchedulerJobInfoRepository extends JpaRepository<SchedulerJobInfo, Long> {
//
//    // ---- Lookup by Quartz identity ----------------------------------------
//
//    Optional<SchedulerJobInfo> findByJobNameAndJobGroup(String jobName, String jobGroup);
//
//    boolean existsByJobNameAndJobGroup(String jobName, String jobGroup);
//
//    // ---- Scoped to DATA_COLLECTION group only (UI table query) -----------
//
//    /**
//     * Main table query — supports the search bar + status filter + pagination.
//     * Only returns rows in the DATA_COLLECTION job group.
//     *
//     * @param search  free-text against name, description, obis_codes (nullable = no filter)
//     * @param status  job_status filter (nullable = no filter)
//     * @param orgId   org filter (nullable = no filter)
//     */
//    @Query("""
//        SELECT j FROM SchedulerJobInfo j
//        WHERE j.jobGroup IN ('DATA_COLLECTION', 'profiles')
//          AND (:orgId IS NULL   OR j.orgId   = :orgId)
//          AND (:status IS NULL  OR j.jobStatus = :status)
//          AND (:search IS NULL  OR :search = ''
//               OR LOWER(j.name)        LIKE LOWER(CONCAT('%', :search, '%'))
//               OR LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%'))
//               OR LOWER(j.obisCodes)   LIKE LOWER(CONCAT('%', :search, '%')))
//        """)
//    Page<SchedulerJobInfo> findDataCollectionSchedules(
//            @Param("search") String search,
//            @Param("status") String status,
//            @Param("orgId")  UUID orgId,
//            Pageable pageable
//    );
//
//    // ---- Bulk status update (Pause / Resume all) -------------------------
//
//    @Modifying
//    @Query("""
//            UPDATE SchedulerJobInfo j
//            SET j.jobStatus = :status,
//                j.updatedAt = CURRENT_TIMESTAMP
//            WHERE j.jobId IN :ids
//            """)
//    int bulkUpdateStatus(@Param("status") String status, @Param("ids") Iterable<Long> ids);
//
//    // ---- Existence check for duplicate name within same org ---------------
//
//    @Query("""
//            SELECT COUNT(j) > 0 FROM SchedulerJobInfo j
//            WHERE LOWER(j.name) = LOWER(:name)
//              AND (j.jobGroup = 'DATA_COLLECTION' or j.jobGroup = 'profiles')
//              AND (:orgId IS NULL OR j.orgId = :orgId)
//              AND j.jobId <> :excludeId
//            """)
//    boolean existsByNameInOrgExcluding(
//            @Param("name")      String name,
//            @Param("orgId")     UUID orgId,
//            @Param("excludeId") Long excludeId
//    );
//
//    /**
//     * Check for duplicate event/profile type name within the same org
//     * (excluding the record being updated).
//     */
//    @Query("""
//            SELECT COUNT(j) > 0 FROM SchedulerJobInfo j
//            WHERE LOWER(j.name) = LOWER(:name)
//              AND (j.jobGroup = 'DATA_COLLECTION' or j.jobGroup = 'profiles')
//              AND (:orgId IS NULL OR j.orgId = :orgId)
//              AND j.jobId <> :excludeId
//            """)
//    boolean existsDuplicateName(
//            @Param("name") String name,
//            @Param("orgId") UUID orgId,
//            @Param("excludeId") Long excludeId
//    );
//}