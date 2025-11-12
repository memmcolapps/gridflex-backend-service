package org.memmcol.gridflexbackendservice.repository;

import org.memmcol.gridflexbackendservice.model.hes.MetersConnectionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetersConnectionEventRepository extends JpaRepository<MetersConnectionEvent, String > {
    @Query("""
        SELECT m 
        FROM MetersConnectionEvent m 
        WHERE m.onlineTime >= :fromTime
        ORDER BY m.onlineTime ASC
    """)
    List<MetersConnectionEvent> findRecentEvents(@Param("fromTime") LocalDateTime fromTime);

    /*✅ Purpose:
	•	Returns the latest connection status for each meter (ONLINE/OFFLINE).
	•	Based on connection_time.*/
    @Query("""
        SELECT e.meterNo, e.connectionType, e.onlineTime
        FROM MetersConnectionEvent e
        WHERE e.onlineTime = (
            SELECT MAX(e2.onlineTime)
            FROM MetersConnectionEvent e2
            WHERE e2.meterNo = e.meterNo
        )
    """)
    List<Object[]> findLatestConnectionEvents();


    @Modifying
    @Query(value = """
   INSERT INTO meters_connection_event (meter_no, connection_type, online_time, offline_time, updated_at)
   VALUES (:meterNo, :status, :connectionTime, :connectionTime, now())
   ON CONFLICT (meter_no) DO UPDATE SET
       connection_type = EXCLUDED.connection_type,
       online_time = EXCLUDED.online_time,
       offline_time = EXCLUDED.offline_time,
       updated_at = EXCLUDED.updated_at
""", nativeQuery = true)
    void upsertConnectionEvent(@Param("meterNo") String meterNo,
                               @Param("status") String status,
                               @Param("connectionTime") LocalDateTime connectionTime);


    @Query("""
        SELECT Count(*)
        FROM MetersConnectionEvent m 
        WHERE m.connectionType = :online
    """)
    Number getActiveMeterCount(String online);
}
