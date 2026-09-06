package com.example.rentalservice.repository;

import com.example.rentalservice.domain.DefectReport;
import com.example.rentalservice.domain.DefectReportStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefectReportRepository extends JpaRepository<DefectReport, Long> {
    List<DefectReport> findAllByStatusOrderByCreatedAtDesc(DefectReportStatus status);
    List<DefectReport> findTop50ByOrderByCreatedAtDesc();
    List<DefectReport> findTop50BySpecialistIdOrderByCreatedAtDesc(Long specialistId);
    List<DefectReport> findAllBySpecialistIdAndStatusOrderByCreatedAtDesc(Long specialistId, DefectReportStatus status);
    long countByStatus(DefectReportStatus status);
    long countBySpecialistIdAndStatus(Long specialistId, DefectReportStatus status);
    boolean existsByServiceRequestIdAndStatusIn(Long serviceRequestId, Set<DefectReportStatus> statuses);

    Optional<DefectReport> findFirstByServiceRequest_IdOrderByCreatedAtDesc(Long serviceRequestId);
}
