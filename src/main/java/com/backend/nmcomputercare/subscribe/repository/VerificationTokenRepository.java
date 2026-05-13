package com.backend.nmcomputercare.subscribe.repository;

import com.backend.nmcomputercare.subscribe.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    /**
     * Removes any previous tokens for this subscriber before issuing a new one.
     * Called inside a @Transactional block in the service.
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.subscription.id = :subscriptionId")
    void deleteBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
