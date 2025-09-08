package org.memmcol.gridflexbackendservice.repository;

import org.apache.ibatis.annotations.Param;
import org.memmcol.gridflexbackendservice.model.audit.ServiceAlertLog;
import org.memmcol.gridflexbackendservice.model.audit.UptimeReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceAlertRepository extends MongoRepository<ServiceAlertLog, String> {
    List<ServiceAlertLog> findByServiceNameAndStartsAtBetween(String serviceName, Instant start, Instant end);

    Optional<ServiceAlertLog> findTopByServiceNameAndStatusAndEndsAtIsNullOrderByStartsAtDesc(String service, String down);

    Optional<ServiceAlertLog> findTopByServiceNameAndStatusOrderByStartsAtDesc(String service, String down);

    @Query("{ 'serviceName': ?0, " +
            "  $or: [ " +
            "    { 'startsAt': { $lt: ?2 }, 'endsAt': { $gt: ?1 } }, " + // log spans across the window
            "    { 'startsAt': { $gte: ?1, $lt: ?2 } } " +              // log starts inside the window
            "  ] " +
            "}")
    List<ServiceAlertLog> findByServiceNameAndTimeRange(
            String serviceName,
            Instant windowStart,
            Instant windowEnd
    );

//    @Query("""
//    SELECT l FROM ServiceAlertLog l
//    WHERE l.serviceName = :serviceName
//      AND l.startsAt < :end
//      AND (l.endsAt IS NULL OR l.endsAt > :start)
//""")
//    List<ServiceAlertLog> findLogsOverlapping(
//            @Param("serviceName") String serviceName,
//            @Param("start") Instant start,
//            @Param("end") Instant end
//    );

}
