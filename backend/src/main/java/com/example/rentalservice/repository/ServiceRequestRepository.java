package com.example.rentalservice.repository;

import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {
    List<ServiceRequest> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ServiceRequest> findByIdAndUserId(Long id, Long userId);
    List<ServiceRequest> findAllByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Set<ServiceRequestStatus> statuses);
    List<ServiceRequest> findAllByStatusInOrderByCreatedAtDesc(Set<ServiceRequestStatus> statuses);
    boolean existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqual(
            Long equipmentId,
            Set<ServiceRequestStatus> statuses,
            LocalDate rentalEndDate,
            LocalDate rentalStartDate
    );
    boolean existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqualAndIdNot(
            Long equipmentId,
            Set<ServiceRequestStatus> statuses,
            LocalDate rentalEndDate,
            LocalDate rentalStartDate,
            Long excludedRequestId
    );
    boolean existsByEquipmentIdAndStatusInAndReturnedAtIsNull(Long equipmentId, Set<ServiceRequestStatus> statuses);
    boolean existsByIdAndUserIdAndReturnedAtIsNull(Long requestId, Long userId);
    List<ServiceRequest> findAllByCreatedAtBetween(Instant from, Instant to);
    long countByStatus(ServiceRequestStatus status);
    @Query("select distinct sr.equipment.id from ServiceRequest sr where sr.status in :statuses and sr.returnedAt is null")
    List<Long> findDistinctEquipmentIdsByStatusInAndReturnedAtIsNull(Set<ServiceRequestStatus> statuses);
}
