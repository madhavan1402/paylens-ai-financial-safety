package com.paylens.backend.repository;

import com.paylens.backend.model.RiskEvent;
import com.paylens.backend.model.RiskEventStatus;
import com.paylens.backend.model.RiskPriority;
import com.paylens.backend.model.RiskSeverity;
import com.paylens.backend.model.RiskSignalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEvent, String> {

    List<RiskEvent> findByFingerprintAndStatusIn(String fingerprint, List<RiskEventStatus> statuses);

    List<RiskEvent> findByStatus(RiskEventStatus status);

    List<RiskEvent> findByStatusInOrderByLastDetectedAtDesc(List<RiskEventStatus> statuses);

    long countByStatus(RiskEventStatus status);

    @Query("SELECT r FROM RiskEvent r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:severity IS NULL OR r.severity = :severity) AND " +
           "(:type IS NULL OR r.riskSignalType = :type) " +
           "ORDER BY r.lastDetectedAt DESC")
    List<RiskEvent> findFiltered(
            @Param("status") RiskEventStatus status,
            @Param("severity") RiskSeverity severity,
            @Param("type") RiskSignalType type
    );
}
