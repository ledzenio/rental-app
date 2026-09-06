package com.example.rentalservice.repository;

import com.example.rentalservice.domain.EquipmentStateHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentStateHistoryRepository extends JpaRepository<EquipmentStateHistory, Long> {
    List<EquipmentStateHistory> findTop20ByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);

    List<EquipmentStateHistory> findTop100ByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);

    Optional<EquipmentStateHistory> findTopByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);
}
