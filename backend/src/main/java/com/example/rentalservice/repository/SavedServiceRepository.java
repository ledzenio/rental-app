package com.example.rentalservice.repository;

import com.example.rentalservice.domain.SavedService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedServiceRepository extends JpaRepository<SavedService, Long> {
    List<SavedService> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);
    void deleteByUserIdAndServiceId(Long userId, Long serviceId);
}
