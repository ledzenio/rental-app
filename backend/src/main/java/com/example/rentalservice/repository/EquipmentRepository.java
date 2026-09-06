package com.example.rentalservice.repository;

import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.EquipmentLifecycleStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findAllByLifecycleStatusOrderByIdAsc(EquipmentLifecycleStatus lifecycleStatus);
    List<Equipment> findAllByServiceCatalogItemIdAndLifecycleStatusOrderByIdAsc(
            Long serviceCatalogItemId,
            EquipmentLifecycleStatus lifecycleStatus
    );
}
