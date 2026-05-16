package com.backend.nmcomputercare.user.service;

import com.backend.nmcomputercare.user.dtos.*;
import com.backend.nmcomputercare.user.entity.User;
import com.backend.nmcomputercare.user.repository.UserRepository;
import com.backend.nmcomputercare.user.validator.UserValidator;
import com.backend.nmcomputercare.utils.*;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin-only user management service: list, find, verify, and delete accounts.
 *
 * <p>All mutating endpoints are protected by {@code ROLE_ADMIN} in
 * {@link com.backend.nmcomputercare.security.config.SecurityConfig}.
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class UserService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String NOT_FOUND_DETAIL =
            "No user account found for the given criteria.";

    private final UserRepository userRepository;
    private final UserValidator  validator;

    // ══════════════════════════════════════════════════════════════════════
    //  Dispatcher
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName",   serviceName);
        try {
            logger.info("UserService invoked | service={} correlationId={}", serviceName, correlationId);
            return switch (serviceName) {
                case "findAllUsers"      -> findAllUsers(toPageable(request));
                case "findUserById"      -> findUserById(request);
                case "updateUserStatus"  -> updateUserStatus(request);
                case "deleteUser"        -> deleteUser(request);
                default -> {
                    ExceptionHandlerReporter.setResolveIssueDetails(
                            "Valid names: findAllUsers, findUserById, updateUserStatus, deleteUser.");
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    throw new ServiceNameNotFoundException("Service name not found: " + serviceName);
                }
            };
        } finally {
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Operations
    // ══════════════════════════════════════════════════════════════════════

    private List<UserResponse> findAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        logger.info("findAllUsers | total={} page={}", page.getTotalElements(), pageable.getPageNumber());
        return toResponseList(page.getContent());
    }

    private List<UserResponse> findUserById(RequestContract request) {
        if (!(request instanceof FindUserByIdRequest req)) throw badRequest();
        User user = userRepository.findById(req.getId())
                .orElseThrow(() -> notFound("id=" + req.getId()));
        return toResponseList(List.of(user));
    }

    /**
     * Verify or unverify a user account.
     * Setting {@code VERIFIED} enables login; {@code NOT_VERIFIED} disables it.
     */
    private List<UserResponse> updateUserStatus(RequestContract request) {
        if (!(request instanceof UpdateUserStatusRequest req)) throw badRequest();
        validator.validateUpdateStatus(req);

        User user = userRepository.findById(req.getId())
                .orElseThrow(() -> notFound("id=" + req.getId()));

        user.setStatus(req.getStatus());
        User saved = userRepository.save(user);
        logger.info("User status updated | id={} status={}", saved.getId(), saved.getStatus());
        return toResponseList(List.of(saved));
    }

    private List<UserResponse> deleteUser(RequestContract request) {
        if (!(request instanceof DeleteUserRequest req)) throw badRequest();
        if (req.getId() == null || req.getId() <= 0) {
            ExceptionHandlerReporter.setResolveIssueDetails("Provide a valid positive id.");
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException("Invalid id for delete: " + req.getId());
        }
        User user = userRepository.findById(req.getId())
                .orElseThrow(() -> notFound("id=" + req.getId()));
        List<UserResponse> snapshot = toResponseList(List.of(user));
        userRepository.delete(user);
        logger.info("User deleted | id={}", req.getId());
        return snapshot;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Mapper
    // ══════════════════════════════════════════════════════════════════════

    private List<UserResponse> toResponseList(List<User> users) {
        return users.stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .surname(u.getSurname())
                        .emailAddress(u.getEmailAddress())
                        .role(u.getRole().name())
                        .roleDisplayName(u.getRole().getDisplayName())
                        .status(u.getStatus().name())
                        .statusDisplayName(u.getStatus().getDisplayName())
                        .registeredDate(u.getRegisteredDate())
                        .build())
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    private Pageable toPageable(RequestContract r) {
        if (r instanceof PageableRequest pr) return pr.toPageable();
        return org.springframework.data.domain.PageRequest.of(0, 20);
    }

    private RuntimeException badRequest() {
        ExceptionHandlerReporter.setResolveIssueDetails(
                "Ensure the correct RequestContract subtype is passed for this service.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        return new IncorrectRequestSentException("Request contract type mismatch.");
    }

    private RuntimeException notFound(String criteria) {
        ExceptionHandlerReporter.setResolveIssueDetails(NOT_FOUND_DETAIL);
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        return new IncorrectRequestSentException("User not found: " + criteria);
    }
}
