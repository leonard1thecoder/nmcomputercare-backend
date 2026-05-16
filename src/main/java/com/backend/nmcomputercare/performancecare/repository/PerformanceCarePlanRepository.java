package com.backend.nmcomputercare.performancecare.repository;

import com.backend.nmcomputercare.performancecare.entity.DeviceType;
import com.backend.nmcomputercare.performancecare.entity.PerformanceCarePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link PerformanceCarePlan}.
 */
@Repository
public interface PerformanceCarePlanRepository extends JpaRepository<PerformanceCarePlan, Long> {

    /** Paginated lookup by lifecycle status code. */
    Page<PerformanceCarePlan> findByStatus(byte status, Pageable pageable);

    /** Paginated lookup by device type (LAPTOP / DESKTOP). */
    Page<PerformanceCarePlan> findByDeviceType(DeviceType deviceType, Pageable pageable);

    /** Count of plans currently in a given status. */
    long countByStatus(byte status);

    /** All plans with a booking date scheduled in the future. */
    @Query("SELECT p FROM PerformanceCarePlan p WHERE p.bookingDate >= :from ORDER BY p.bookingDate ASC")
    List<PerformanceCarePlan> findUpcomingAppointments(@Param("from") LocalDateTime from);

    /** Most recent {@code n} plans — useful for admin dashboards. */
    @Query("SELECT p FROM PerformanceCarePlan p ORDER BY p.createdDate DESC")
    List<PerformanceCarePlan> findRecentPlans(Pageable pageable);
}
