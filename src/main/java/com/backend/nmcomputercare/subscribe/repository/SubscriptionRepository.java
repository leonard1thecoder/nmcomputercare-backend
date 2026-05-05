package com.backend.nmcomputercare.subscribe.repository;


import com.backend.nmcomputercare.subscribe.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByEmail(String email);

    List<Subscription> findByActive(boolean active);

    Page<Subscription> findAll(Pageable pageable);

    boolean existsByEmail(String email);
}
