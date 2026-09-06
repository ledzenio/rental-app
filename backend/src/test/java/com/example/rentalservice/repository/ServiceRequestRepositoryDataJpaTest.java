package com.example.rentalservice.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rentalservice.domain.Equipment;
import com.example.rentalservice.domain.EquipmentLifecycleStatus;
import com.example.rentalservice.domain.RoleName;
import com.example.rentalservice.domain.ServiceCatalogItem;
import com.example.rentalservice.domain.ServiceRequest;
import com.example.rentalservice.domain.ServiceRequestStatus;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.domain.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ServiceRequestRepositoryDataJpaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("repo@test.local");
        user.setPasswordHash("hash");
        user.setFullName("Repo User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(RoleName.USER);
        user.setCreatedAt(Instant.now());
        user.setVirtualBalance(new BigDecimal("1000.00"));
        User savedUser = userRepository.save(user);

        ServiceCatalogItem item = new ServiceCatalogItem();
        item.setTitle("Аренда генератора");
        item.setDescription("Описание");
        item.setCategory("Генераторы");
        item.setBasePrice(new BigDecimal("120.00"));
        item.setActive(true);
        item.setCreatedAt(Instant.now());
        ServiceCatalogItem savedItem = serviceCatalogRepository.save(item);

        Equipment eq = new Equipment();
        ReflectionTestUtils.setField(eq, "inventoryCode", "TEST-001");
        ReflectionTestUtils.setField(eq, "modelName", "Test Model");
        ReflectionTestUtils.setField(eq, "purchasePrice", new BigDecimal("10000.00"));
        ReflectionTestUtils.setField(eq, "baseWearPercent", new BigDecimal("5.00"));
        eq.setServiceCatalogItem(savedItem);
        eq.setLifecycleStatus(EquipmentLifecycleStatus.AVAILABLE);
        eq.setAccumulatedWearPercent(new BigDecimal("0.00"));
        eq.setRentalWearRatePerDay(new BigDecimal("0.1000"));
        equipment = equipmentRepository.save(eq);

        ServiceRequest request = new ServiceRequest();
        request.setUser(savedUser);
        request.setService(savedItem);
        request.setEquipment(equipment);
        request.setNotes("test");
        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request.setCreatedAt(Instant.now());
        request.setRentalStartDate(LocalDate.of(2026, 5, 10));
        request.setRentalEndDate(LocalDate.of(2026, 5, 12));
        request.setShiftsPerDay(1);
        serviceRequestRepository.save(request);
    }

    @Test
    void overlapCheckReturnsTrueForIntersectingPeriod() {
        boolean busy = serviceRequestRepository.existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqual(
                equipment.getId(),
                Set.of(ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.NEW),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 9)
        );

        assertTrue(busy);
    }

    @Test
    void overlapCheckReturnsFalseForNonIntersectingPeriod() {
        boolean busy = serviceRequestRepository.existsByEquipmentIdAndStatusInAndRentalStartDateLessThanEqualAndRentalEndDateGreaterThanEqual(
                equipment.getId(),
                Set.of(ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.NEW),
                LocalDate.of(2026, 5, 16),
                LocalDate.of(2026, 5, 14)
        );

        assertFalse(busy);
    }
}
