package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.audit.UptimeReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

public interface UptimeReportRepository extends MongoRepository<UptimeReport, String> {

    Optional<UptimeReport> findByServiceNameAndReportTypeAndCreatedAt(String serviceName, String daily, String date);

    Optional<UptimeReport> findByServiceNameAndReportTypeAndMonth(String serviceName, String monthly, String string);
}
