package com.backend.nmcomputercare.basiccare.repository;

import com.backend.nmcomputercare.basiccare.entity.BasicCarePlan;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link BasicCarePlan}.
 *
 * <p>Custom queries are kept minimal and expressed in JPQL so that the
 * underlying dialect can be swapped without changes here.
 */
@Repository
public interface BasicCarePlanRepository extends JpaRepository<BasicCarePlan, Long> {

    /**
     * Returns all plans in a given lifecycle status, paginated.
     *
     * @param status numeric status code — see {@link com.backend.nmcomputercare.basiccare.entity.BasicCareStatus}
     */
    Page<BasicCarePlan> findByStatus(byte status, Pageable pageable);

    /**
     * Returns all plans for a specific operating system, paginated.
     */
    Page<BasicCarePlan> findByOperationSystem(OperationSystem operationSystem, Pageable pageable);

    /**
     * Counts plans currently in a given status.  Useful for dashboard stats.
     */
    long countByStatus(byte status);

    /**
     * Returns all plans that have a screenshot on file.
     */
    @Query("SELECT b FROM BasicCarePlan b WHERE b.screenShotFilePath IS NOT NULL")
    List<BasicCarePlan> findAllWithScreenshots();

    /**
     * Returns the most recent {@code limit} plans — handy for admin dashboards.
     */
    @Query("SELECT b FROM BasicCarePlan b ORDER BY b.createdDate DESC")
    List<BasicCarePlan> findRecentPlans(Pageable pageable);

    /**
     * Full-text search against the issue description (case-insensitive).
     */
    @Query("SELECT b FROM BasicCarePlan b WHERE LOWER(b.issueDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<BasicCarePlan> searchByIssueDescription(@Param("keyword") String keyword, Pageable pageable);
}
