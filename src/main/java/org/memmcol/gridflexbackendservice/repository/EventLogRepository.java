package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.hes.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    /*✅ Purpose:
	•	Returns the latest tamper or relay event for each meter.
	•	Event types:
	•	3 = Tamper
	•	4 = Relay/Control
	*/
    @Query("""
        SELECT e.meterSerial, e.eventName, e.eventTime
        FROM EventLog e
        WHERE e.eventType = :eventTypeId
        AND e.eventTime = (
            SELECT MAX(e2.eventTime)
            FROM EventLog e2
            WHERE e2.meterSerial = e.meterSerial
            AND e2.eventType = :eventTypeId
        )
    """)
    List<Object[]> findLatestEventLogsByType(@Param("eventTypeId") int eventTypeId);
}
