package com.example.rentalservice.repository;

import com.example.rentalservice.domain.Invoice;
import com.example.rentalservice.domain.InvoiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @EntityGraph(attributePaths = {"serviceRequest", "serviceRequest.service", "defectReport"})
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId ORDER BY i.createdAt DESC")
    List<Invoice> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    List<Invoice> findAllByStatusOrderByCreatedAtDesc(InvoiceStatus status);
    List<Invoice> findAllByStatusAndPaidAtBetweenOrderByPaidAtAsc(InvoiceStatus status, Instant from, Instant to);
    List<Invoice> findAllByCreatedAtBetween(Instant from, Instant to);
    boolean existsByServiceRequestIdAndStatus(Long serviceRequestId, InvoiceStatus status);
    boolean existsByServiceRequestId(Long serviceRequestId);

    boolean existsByUser_IdAndStatus(Long userId, InvoiceStatus status);

    boolean existsByDefectReport_Id(Long defectReportId);

    Optional<Invoice> findTopByDefectReport_IdOrderByCreatedAtDesc(Long defectReportId);
}
