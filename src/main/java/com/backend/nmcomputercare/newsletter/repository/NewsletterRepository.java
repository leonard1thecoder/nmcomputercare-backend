package com.backend.nmcomputercare.newsletter.repository;

import com.backend.nmcomputercare.newsletter.entity.Newsletter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {



    /** Returns all newsletters with the given publication status. */
    List<Newsletter> findByStatus(byte status);

    /** Paginated variant for findAll (JpaRepository already provides one). */
    Page<Newsletter> findAll(Pageable pageable);
}
