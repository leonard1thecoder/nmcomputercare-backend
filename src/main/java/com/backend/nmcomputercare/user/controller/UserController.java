package com.backend.nmcomputercare.user.controller;

import com.backend.nmcomputercare.user.dtos.*;
import com.backend.nmcomputercare.user.entity.UserStatus;
import com.backend.nmcomputercare.user.service.UserService;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.ExecService;
import com.backend.nmcomputercare.utils.ResponseContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Admin-only user management endpoints.
 * Base path: {@code /api/v1/users}
 *
 * <p>All endpoints require {@code ROLE_ADMIN} — enforced both in
 * {@link com.backend.nmcomputercare.security.config.SecurityConfig} (URL-level)
 * and via {@link PreAuthorize} (method-level, defence-in-depth).
 *
 * <pre>
 *  GET    /api/v1/users                → paginated list of all users
 *  GET    /api/v1/users/{id}           → find by id
 *  PATCH  /api/v1/users/{id}/status    → verify or unverify an account
 *  DELETE /api/v1/users/{id}           → hard-delete a user
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController extends ExecService {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService,
                          ExecutorService controllerExecutorService,
                          ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.userService = userService;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/users
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Paginated list of all registered users.
     *
     * <p>Example: {@code GET /api/v1/users?page=0&size=10&sortBy=registeredDate&direction=DESC}
     */
    @GetMapping
    public ResponseEntity<PagedUserResponse> findAll(
            @RequestParam(defaultValue = "0")              int            page,
            @RequestParam(defaultValue = "20")             int            size,
            @RequestParam(defaultValue = "registeredDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")           Sort.Direction direction) {

        logger.info("GET /users | page={} size={}", page, size);

        FindAllUsersRequest request = FindAllUsersRequest.builder()
                .page(page).size(size).sortBy(sortBy).direction(direction).build();

        @SuppressWarnings("unchecked")
        List<UserResponse> content =
                (List<UserResponse>) exec(userService, "findAllUsers", request);

        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;

        return ResponseEntity.ok(PagedUserResponse.builder()
                .content(content)
                .page(page).size(safeSize)
                .totalItems(totalItems).totalPages(totalPages)
                .first(page == 0).last(page >= totalPages - 1)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/users/{id}
    // ══════════════════════════════════════════════════════════════════════

    /** Retrieve a single user by primary key. */
    @GetMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> findById(@PathVariable Long id) {
        logger.info("GET /users/{}", id);
        return ResponseEntity.ok(exec(userService, "findUserById",
                FindUserByIdRequest.builder().id(id).build()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PATCH  /api/v1/users/{id}/status
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verify or unverify a user account.
     *
     * <p>Set status to {@code VERIFIED} to allow login.
     * Set to {@code NOT_VERIFIED} to suspend the account immediately
     * (existing tokens will still pass validation until they expire — issue
     * a token revocation mechanism if instant lock-out is required).
     *
     * @param status {@code VERIFIED} or {@code NOT_VERIFIED}
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<List<? extends ResponseContract>> updateStatus(
            @PathVariable Long       id,
            @RequestParam UserStatus status) {

        logger.info("PATCH /users/{}/status | status={}", id, status);
        return ResponseEntity.ok(exec(userService, "updateUserStatus",
                UpdateUserStatusRequest.builder().id(id).status(status).build()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE  /api/v1/users/{id}
    // ══════════════════════════════════════════════════════════════════════

    /** Hard-delete a user account.  Returns a snapshot of the deleted record. */
    @DeleteMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> delete(@PathVariable Long id) {
        logger.info("DELETE /users/{}", id);
        return ResponseEntity.ok(exec(userService, "deleteUser",
                DeleteUserRequest.builder().id(id).build()));
    }
}
