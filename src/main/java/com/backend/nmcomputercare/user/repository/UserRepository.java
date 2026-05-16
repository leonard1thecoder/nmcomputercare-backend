package com.backend.nmcomputercare.user.repository;

import com.backend.nmcomputercare.user.entity.Privilege;
import com.backend.nmcomputercare.user.entity.User;
import com.backend.nmcomputercare.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by Spring Security's {@code UserDetailsService} to load by login email. */
    Optional<User> findByEmailAddress(String emailAddress);

    boolean existsByEmailAddress(String emailAddress);

    Page<User> findByRole(Privilege role, Pageable pageable);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    long countByRole(Privilege role);

    long countByStatus(UserStatus status);
}
