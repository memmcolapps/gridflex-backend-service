package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.audit.UptimeReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

public interface UptimeReportRepository extends MongoRepository<UptimeReport, String> {
    Optional<UptimeReport> findByServiceNameAndCreatedAt(String serviceName, LocalDate createdAt);

    Optional<UptimeReport> findByServiceNameAndMonth(String serviceName, String month);

    Optional<UptimeReport> findByServiceNameAndReportTypeAndCreatedAt(String serviceName, String daily, LocalDate date);

    Optional<UptimeReport> findByServiceNameAndReportTypeAndCreatedAtMonth(String serviceName, String reportType, YearMonth month);

}
