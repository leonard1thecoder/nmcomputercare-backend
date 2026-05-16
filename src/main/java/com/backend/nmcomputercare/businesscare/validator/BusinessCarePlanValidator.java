package com.backend.nmcomputercare.businesscare.validator;

import com.backend.nmcomputercare.basiccare.entity.*;
import com.backend.nmcomputercare.businesscare.dtos.CreateBusinessCarePlanRequest;
import com.backend.nmcomputercare.businesscare.dtos.UpdateBusinessCarePlanRequest;
import com.backend.nmcomputercare.businesscare.entity.BulkType;
import com.backend.nmcomputercare.businesscare.entity.BusinessCareStatus;
import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates incoming Business Care Plan requests.
 *
 * <h3>Core rules</h3>
 * <ul>
 *   <li>bulkType and quantity are always required.</li>
 *   <li>When {@code PERFORMANCE_CARE}: all Performance Care fields required with
 *       consistency checks; all Basic Care fields must be their NONE/null defaults.</li>
 *   <li>When {@code BASIC_CARE}: all Basic Care fields required;
 *       all Performance Care fields must be NONE.</li>
 *   <li>CPU brand ↔ model consistency (same rules as PerformanceCare validator).</li>
 *   <li>RAM generation ↔ capacity consistency.</li>
 *   <li>GPU upgrades not permitted on laptops.</li>
 *   <li>At least one upgrade must be selected for PERFORMANCE_CARE.</li>
 *   <li>bookingDate, when supplied, must be in the future.</li>
 * </ul>
 */
@Component
public class BusinessCarePlanValidator {

    private static final int MAX_DESC_LEN = 2_000;
    private static final int MAX_PATH_LEN = 500;

    // ── Create ─────────────────────────────────────────────────────────────

    public void validateCreate(CreateBusinessCarePlanRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getBulkType() == null) {
            errors.add("bulkType must not be null.");
            failIfErrors(errors); // cannot proceed without bulkType
        }
        if (req.getQuantity() == null || req.getQuantity() < 1) {
            errors.add("quantity must be a positive integer ≥ 1.");
        }

        if (req.getBulkType() == BulkType.PERFORMANCE_CARE) {
            validatePerformanceFields(
                    req.getCpuUpdate(), req.getIntelModel(), req.getAmdModel(),
                    req.getRamUpdate(), req.getDdr2Ram(), req.getDdr3Ram(), req.getDdr4Ram(),
                    req.getDeviceType(), req.getUpgradeGraphicCard(), errors);
            assertBasicFieldsAreDefault(req, errors);
        } else {
            validateBasicFields(
                    req.getOperationSystem(), req.getUpgradeDrivers(),
                    req.getAdditionalPerformanceSoftware(), req.getIssueDescription(),
                    req.getScreenShotFilePath(), req.getDisplayStatus(),
                    req.getComputerType(), errors);
            assertPerformanceFieldsAreDefault(req, errors);
        }

        validateBookingDate(req.getBookingDate(), errors);
        failIfErrors(errors);
    }

    // ── Update ─────────────────────────────────────────────────────────────

    public void validateUpdate(UpdateBusinessCarePlanRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getId() == null || req.getId() <= 0) {
            errors.add("id must be a positive non-null value.");
        }
        if (req.getQuantity() != null && req.getQuantity() < 1) {
            errors.add("quantity must be ≥ 1 when provided.");
        }
        if (req.getStatus() != null) {
            try { BusinessCareStatus.fromCode(req.getStatus()); }
            catch (IllegalArgumentException e) {
                errors.add("status code " + req.getStatus() + " is invalid. Valid range: 0–5.");
            }
        }
        validateBookingDate(req.getBookingDate(), errors);

        // If bulkType is changing, enforce the cross-section rules on supplied fields.
        if (req.getBulkType() == BulkType.PERFORMANCE_CARE
                && anyPerformanceFieldPresent(req)) {
            // Minimal check: if any CPU field is present, all three must be.
            if (req.getCpuUpdate() != null || req.getIntelModel() != null || req.getAmdModel() != null) {
                requireNotNull(req.getCpuUpdate(),  "cpuUpdate",  errors);
                requireNotNull(req.getIntelModel(), "intelModel", errors);
                requireNotNull(req.getAmdModel(),   "amdModel",   errors);
                if (errors.isEmpty())
                    validateCpuConsistency(req.getCpuUpdate(), req.getIntelModel(), req.getAmdModel(), errors);
            }
        }

        failIfErrors(errors);
    }

    // ── Performance Care section ────────────────────────────────────────────

    private void validatePerformanceFields(
            CpuUpdate cpuUpdate, IntelModel intelModel, AmdModel amdModel,
            RamUpdate ramUpdate, Ddr2Ram ddr2, Ddr3Ram ddr3, Ddr4Ram ddr4,
            DeviceType deviceType, UpgradeGraphicCard gpu,
            List<String> errors) {

        requireNotNull(cpuUpdate,   "cpuUpdate",          errors);
        requireNotNull(intelModel,  "intelModel",         errors);
        requireNotNull(amdModel,    "amdModel",           errors);
        requireNotNull(ramUpdate,   "ramUpdate",          errors);
        requireNotNull(ddr2,        "ddr2Ram",            errors);
        requireNotNull(ddr3,        "ddr3Ram",            errors);
        requireNotNull(ddr4,        "ddr4Ram",            errors);
        requireNotNull(deviceType,  "deviceType",         errors);
        requireNotNull(gpu,         "upgradeGraphicCard", errors);

        if (errors.stream().noneMatch(e -> e.contains("null"))) {
            validateCpuConsistency(cpuUpdate, intelModel, amdModel, errors);
            validateRamConsistency(ramUpdate, ddr2, ddr3, ddr4, errors);
            if (deviceType == DeviceType.LAPTOP && gpu != UpgradeGraphicCard.NONE)
                errors.add("upgradeGraphicCard must be NONE for LAPTOP devices.");
            if (cpuUpdate == CpuUpdate.NONE && ramUpdate == RamUpdate.NONE
                    && gpu == UpgradeGraphicCard.NONE)
                errors.add("At least one upgrade must be selected: CPU, RAM, or GPU.");
        }
    }

    private void assertBasicFieldsAreDefault(CreateBusinessCarePlanRequest req, List<String> errors) {
        if (req.getOperationSystem() != null && req.getOperationSystem() != OperationSystem.NO_OPERATING_SYSTEM)
            errors.add("operationSystem must be NO_OPERATING_SYSTEM when bulkType is PERFORMANCE_CARE.");
        if (req.getUpgradeDrivers() != null && req.getUpgradeDrivers() != UpgradeDrivers.NO)
            errors.add("upgradeDrivers must be NO when bulkType is PERFORMANCE_CARE.");
        if (req.getAdditionalPerformanceSoftware() != null
                && req.getAdditionalPerformanceSoftware() != AdditionalPerformanceSoftware.NONE)
            errors.add("additionalPerformanceSoftware must be NONE when bulkType is PERFORMANCE_CARE.");
        if (req.getIssueDescription() != null && !req.getIssueDescription().isBlank())
            errors.add("issueDescription must be empty when bulkType is PERFORMANCE_CARE.");
    }

    // ── Basic Care section ──────────────────────────────────────────────────

    private void validateBasicFields(
            OperationSystem os, UpgradeDrivers drivers,
            AdditionalPerformanceSoftware software,
            String issueDesc, String screenshotPath,
            DisplayStatus displayStatus, ComputerType computerType,
            List<String> errors) {

        requireNotNull(os,            "operationSystem",              errors);
        requireNotNull(drivers,       "upgradeDrivers",               errors);
        requireNotNull(software,      "additionalPerformanceSoftware",errors);
        requireNotNull(displayStatus, "displayStatus",                errors);
        requireNotNull(computerType,  "computerType",                 errors);

        if (issueDesc == null || issueDesc.isBlank())
            errors.add("issueDescription must not be blank for BASIC_CARE.");
        else if (issueDesc.length() > MAX_DESC_LEN)
            errors.add("issueDescription must not exceed " + MAX_DESC_LEN + " characters.");

        if (screenshotPath != null && screenshotPath.length() > MAX_PATH_LEN)
            errors.add("screenShotFilePath must not exceed " + MAX_PATH_LEN + " characters.");
    }

    private void assertPerformanceFieldsAreDefault(CreateBusinessCarePlanRequest req, List<String> errors) {
        if (req.getCpuUpdate() != null && req.getCpuUpdate() != CpuUpdate.NONE)
            errors.add("cpuUpdate must be NONE when bulkType is BASIC_CARE.");
        if (req.getIntelModel() != null && req.getIntelModel() != IntelModel.NONE)
            errors.add("intelModel must be NONE when bulkType is BASIC_CARE.");
        if (req.getAmdModel() != null && req.getAmdModel() != AmdModel.NONE)
            errors.add("amdModel must be NONE when bulkType is BASIC_CARE.");
        if (req.getRamUpdate() != null && req.getRamUpdate() != RamUpdate.NONE)
            errors.add("ramUpdate must be NONE when bulkType is BASIC_CARE.");
        if (req.getUpgradeGraphicCard() != null && req.getUpgradeGraphicCard() != UpgradeGraphicCard.NONE)
            errors.add("upgradeGraphicCard must be NONE when bulkType is BASIC_CARE.");
    }

    // ── Shared cross-field helpers ──────────────────────────────────────────

    private void validateCpuConsistency(CpuUpdate brand, IntelModel intel,
                                        AmdModel amd, List<String> errors) {
        switch (brand) {
            case NONE  -> {
                if (intel != IntelModel.NONE) errors.add("intelModel must be NONE when cpuUpdate is NONE.");
                if (amd   != AmdModel.NONE)   errors.add("amdModel must be NONE when cpuUpdate is NONE.");
            }
            case INTEL -> {
                if (intel == IntelModel.NONE) errors.add("intelModel must not be NONE when cpuUpdate is INTEL.");
                if (amd   != AmdModel.NONE)   errors.add("amdModel must be NONE when cpuUpdate is INTEL.");
            }
            case AMD   -> {
                if (amd   == AmdModel.NONE)   errors.add("amdModel must not be NONE when cpuUpdate is AMD.");
                if (intel != IntelModel.NONE) errors.add("intelModel must be NONE when cpuUpdate is AMD.");
            }
        }
    }

    private void validateRamConsistency(RamUpdate gen, Ddr2Ram d2,
                                        Ddr3Ram d3, Ddr4Ram d4, List<String> errors) {
        switch (gen) {
            case NONE -> {
                if (d2 != Ddr2Ram.NONE) errors.add("ddr2Ram must be NONE when ramUpdate is NONE.");
                if (d3 != Ddr3Ram.NONE) errors.add("ddr3Ram must be NONE when ramUpdate is NONE.");
                if (d4 != Ddr4Ram.NONE) errors.add("ddr4Ram must be NONE when ramUpdate is NONE.");
            }
            case DDR2 -> {
                if (d2 == Ddr2Ram.NONE) errors.add("ddr2Ram must not be NONE when ramUpdate is DDR2.");
                if (d3 != Ddr3Ram.NONE) errors.add("ddr3Ram must be NONE when ramUpdate is DDR2.");
                if (d4 != Ddr4Ram.NONE) errors.add("ddr4Ram must be NONE when ramUpdate is DDR2.");
            }
            case DDR3 -> {
                if (d3 == Ddr3Ram.NONE) errors.add("ddr3Ram must not be NONE when ramUpdate is DDR3.");
                if (d2 != Ddr2Ram.NONE) errors.add("ddr2Ram must be NONE when ramUpdate is DDR3.");
                if (d4 != Ddr4Ram.NONE) errors.add("ddr4Ram must be NONE when ramUpdate is DDR3.");
            }
            case DDR4 -> {
                if (d4 == Ddr4Ram.NONE) errors.add("ddr4Ram must not be NONE when ramUpdate is DDR4.");
                if (d2 != Ddr2Ram.NONE) errors.add("ddr2Ram must be NONE when ramUpdate is DDR4.");
                if (d3 != Ddr3Ram.NONE) errors.add("ddr3Ram must be NONE when ramUpdate is DDR4.");
            }
        }
    }

    private void validateBookingDate(LocalDateTime date, List<String> errors) {
        if (date != null && date.isBefore(LocalDateTime.now()))
            errors.add("bookingDate must be in the future.");
    }

    private boolean anyPerformanceFieldPresent(UpdateBusinessCarePlanRequest req) {
        return req.getCpuUpdate() != null || req.getIntelModel() != null
                || req.getAmdModel() != null || req.getRamUpdate() != null
                || req.getUpgradeGraphicCard() != null;
    }

    private void requireNotNull(Object v, String field, List<String> errors) {
        if (v == null) errors.add(field + " must not be null.");
    }

    private void failIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            String detail = String.join(" | ", errors);
            ExceptionHandlerReporter.setResolveIssueDetails(detail);
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException(
                    "Validation failed for BusinessCarePlan: " + detail);
        }
    }
}
