package com.example.rentalservice.repository;

import com.example.rentalservice.domain.EquipmentWearLedger;
import com.example.rentalservice.domain.WearLedgerEntryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentWearLedgerRepository extends JpaRepository<EquipmentWearLedger, Long> {
    boolean existsByEntryTypeAndServiceRequest_Id(WearLedgerEntryType entryType, Long serviceRequestId);

    boolean existsByEntryTypeAndDefectReport_Id(WearLedgerEntryType entryType, Long defectReportId);

    List<EquipmentWearLedger> findTop50ByEquipment_IdOrderByCreatedAtDesc(Long equipmentId);
}
