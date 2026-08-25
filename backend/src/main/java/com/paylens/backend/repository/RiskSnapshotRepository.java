package com.paylens.backend.repository;

import com.paylens.backend.model.RiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RiskSnapshotRepository extends JpaRepository<RiskSnapshot, String> {
    Optional<RiskSnapshot> findTopByOrderByCapturedAtDesc();
}
