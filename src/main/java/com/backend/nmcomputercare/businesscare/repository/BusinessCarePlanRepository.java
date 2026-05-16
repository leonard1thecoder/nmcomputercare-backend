package com.backend.nmcomputercare.businesscare.repository;

import com.backend.nmcomputercare.businesscare.entity.BulkType;
import com.backend.nmcomputercare.businesscare.entity.BusinessCarePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link BusinessCarePlan}.
 */
@Repository
public interface BusinessCarePlanRepository extends JpaRepository<BusinessCarePlan, Long> {

    /** Paginated lookup by lifecycle status code. */
    Page<BusinessCarePlan> findByStatus(byte status, Pageable pageable);

    /** Paginated lookup by service category (PERFORMANCE_CARE or BASIC_CARE). */
    Page<BusinessCarePlan> findByBulkType(BulkType bulkType, Pageable pageable);

    /** Paginated lookup by both status and service category. */
    Page<BusinessCarePlan> findByStatusAndBulkType(byte status, BulkType bulkType, Pageable pageable);

    /** Count plans in a given status. */
    long countByStatus(byte status);

    /** All plans with an upcoming appointment. */
    @Query("SELECT b FROM BusinessCarePlan b WHERE b.bookingDate >= :from ORDER BY b.bookingDate ASC")
    List<BusinessCarePlan> findUpcomingAppointments(@Param("from") LocalDateTime from);

    /** Most recent {@code n} plans for admin dashboards. */
    @Query("SELECT b FROM BusinessCarePlan b ORDER BY b.createdDate DESC")
    List<BusinessCarePlan> findRecentPlans(Pageable pageable);
}
