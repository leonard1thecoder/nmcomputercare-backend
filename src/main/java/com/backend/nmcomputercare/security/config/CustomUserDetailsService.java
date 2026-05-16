package com.backend.nmcomputercare.security.config;

import com.backend.nmcomputercare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a {@link com.backend.nmcomputercare.user.entity.User} from the database
 * by email address for Spring Security's authentication pipeline.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param username the login email address
     * @throws UsernameNotFoundException when no account exists for the given email
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailAddress(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for email: " + username));
    }
}
