package com.backend.nmcomputercare.performancecare.validator;

import com.backend.nmcomputercare.performancecare.dtos.CreatePerformanceCarePlanRequest;
import com.backend.nmcomputercare.performancecare.dtos.UpdatePerformanceCarePlanRequest;
import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates incoming Performance Care Plan requests.
 *
 * <p>All violations are collected before throwing so the client receives
 * a complete list of problems in a single response.
 *
 * <h3>Cross-field rules</h3>
 * <ul>
 *   <li>CPU brand and model must be consistent (INTEL ↔ IntelModel, AMD ↔ AmdModel).</li>
 *   <li>RAM type and capacity must be consistent (DDR2/3/4 ↔ matching DDR enum).</li>
 *   <li>GPU upgrade is not permitted when deviceType is LAPTOP.</li>
 *   <li>At least one upgrade (CPU, RAM, or GPU) must be selected.</li>
 * </ul>
 */
@Component
public class PerformanceCarePlanValidator {

    // ── Create ─────────────────────────────────────────────────────────────

    public void validateCreate(CreatePerformanceCarePlanRequest req) {
        List<String> errors = new ArrayList<>();

        requireNotNull(req.getCpuUpdate(),          "cpuUpdate",          errors);
        requireNotNull(req.getIntelModel(),          "intelModel",         errors);
        requireNotNull(req.getAmdModel(),            "amdModel",           errors);
        requireNotNull(req.getRamUpdate(),           "ramUpdate",          errors);
        requireNotNull(req.getDdr2Ram(),             "ddr2Ram",            errors);
        requireNotNull(req.getDdr3Ram(),             "ddr3Ram",            errors);
        requireNotNull(req.getDdr4Ram(),             "ddr4Ram",            errors);
        requireNotNull(req.getDeviceType(),          "deviceType",         errors);
        requireNotNull(req.getUpgradeGraphicCard(),  "upgradeGraphicCard", errors);

        // Only validate cross-field rules if all fields are present.
        if (errors.isEmpty()) {
            validateCpuConsistency(req.getCpuUpdate(), req.getIntelModel(), req.getAmdModel(), errors);
            validateRamConsistency(req.getRamUpdate(), req.getDdr2Ram(), req.getDdr3Ram(), req.getDdr4Ram(), errors);
            validateGpuLaptopRule(req.getDeviceType(), req.getUpgradeGraphicCard(), errors);
            validateAtLeastOneUpgrade(req.getCpuUpdate(), req.getRamUpdate(), req.getUpgradeGraphicCard(), errors);
            validateBookingDate(req.getBookingDate(), errors);
        }

        failIfErrors(errors);
    }

    // ── Update ─────────────────────────────────────────────────────────────

    public void validateUpdate(UpdatePerformanceCarePlanRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getId() == null || req.getId() <= 0) {
            errors.add("id must be a positive non-null value.");
        }

        // Cross-field rules only apply when the related fields are all provided together.
        boolean cpuFieldsProvided = req.getCpuUpdate()   != null
                || req.getIntelModel()  != null
                || req.getAmdModel()    != null;

        boolean ramFieldsProvided = req.getRamUpdate()   != null
                || req.getDdr2Ram()     != null
                || req.getDdr3Ram()     != null
                || req.getDdr4Ram()     != null;

        if (cpuFieldsProvided) {
            // If any CPU field is being updated, all three must be provided.
            requireNotNull(req.getCpuUpdate(),  "cpuUpdate (required when updating CPU)", errors);
            requireNotNull(req.getIntelModel(), "intelModel (required when updating CPU)", errors);
            requireNotNull(req.getAmdModel(),   "amdModel (required when updating CPU)", errors);
            if (errors.isEmpty()) {
                validateCpuConsistency(req.getCpuUpdate(), req.getIntelModel(), req.getAmdModel(), errors);
            }
        }
        if (ramFieldsProvided) {
            requireNotNull(req.getRamUpdate(), "ramUpdate (required when updating RAM)", errors);
            requireNotNull(req.getDdr2Ram(),   "ddr2Ram (required when updating RAM)", errors);
            requireNotNull(req.getDdr3Ram(),   "ddr3Ram (required when updating RAM)", errors);
            requireNotNull(req.getDdr4Ram(),   "ddr4Ram (required when updating RAM)", errors);
            if (errors.isEmpty()) {
                validateRamConsistency(req.getRamUpdate(), req.getDdr2Ram(), req.getDdr3Ram(), req.getDdr4Ram(), errors);
            }
        }
        if (req.getDeviceType() != null && req.getUpgradeGraphicCard() != null) {
            validateGpuLaptopRule(req.getDeviceType(), req.getUpgradeGraphicCard(), errors);
        }
        if (req.getStatus() != null) {
            try {
                PerformanceCareStatus.fromCode(req.getStatus());
            } catch (IllegalArgumentException e) {
                errors.add("status code " + req.getStatus() + " is invalid. Valid codes: 0–5.");
            }
        }
        validateBookingDate(req.getBookingDate(), errors);

        failIfErrors(errors);
    }

    // ── Cross-field rules ──────────────────────────────────────────────────

    private void validateCpuConsistency(CpuUpdate brand,
                                        IntelModel intel, AmdModel amd,
                                        List<String> errors) {
        switch (brand) {
            case NONE -> {
                if (intel != IntelModel.NONE)
                    errors.add("intelModel must be NONE when cpuUpdate is NONE.");
                if (amd != AmdModel.NONE)
                    errors.add("amdModel must be NONE when cpuUpdate is NONE.");
            }
            case INTEL -> {
                if (intel == IntelModel.NONE)
                    errors.add("intelModel must not be NONE when cpuUpdate is INTEL.");
                if (amd != AmdModel.NONE)
                    errors.add("amdModel must be NONE when cpuUpdate is INTEL.");
            }
            case AMD -> {
                if (amd == AmdModel.NONE)
                    errors.add("amdModel must not be NONE when cpuUpdate is AMD.");
                if (intel != IntelModel.NONE)
                    errors.add("intelModel must be NONE when cpuUpdate is AMD.");
            }
        }
    }

    private void validateRamConsistency(RamUpdate gen,
                                        Ddr2Ram ddr2, Ddr3Ram ddr3, Ddr4Ram ddr4,
                                        List<String> errors) {
        switch (gen) {
            case NONE -> {
                if (ddr2 != Ddr2Ram.NONE) errors.add("ddr2Ram must be NONE when ramUpdate is NONE.");
                if (ddr3 != Ddr3Ram.NONE) errors.add("ddr3Ram must be NONE when ramUpdate is NONE.");
                if (ddr4 != Ddr4Ram.NONE) errors.add("ddr4Ram must be NONE when ramUpdate is NONE.");
            }
            case DDR2 -> {
                if (ddr2 == Ddr2Ram.NONE) errors.add("ddr2Ram must not be NONE when ramUpdate is DDR2.");
                if (ddr3 != Ddr3Ram.NONE) errors.add("ddr3Ram must be NONE when ramUpdate is DDR2.");
                if (ddr4 != Ddr4Ram.NONE) errors.add("ddr4Ram must be NONE when ramUpdate is DDR2.");
            }
            case DDR3 -> {
                if (ddr3 == Ddr3Ram.NONE) errors.add("ddr3Ram must not be NONE when ramUpdate is DDR3.");
                if (ddr2 != Ddr2Ram.NONE) errors.add("ddr2Ram must be NONE when ramUpdate is DDR3.");
                if (ddr4 != Ddr4Ram.NONE) errors.add("ddr4Ram must be NONE when ramUpdate is DDR3.");
            }
            case DDR4 -> {
                if (ddr4 == Ddr4Ram.NONE) errors.add("ddr4Ram must not be NONE when ramUpdate is DDR4.");
                if (ddr2 != Ddr2Ram.NONE) errors.add("ddr2Ram must be NONE when ramUpdate is DDR4.");
                if (ddr3 != Ddr3Ram.NONE) errors.add("ddr3Ram must be NONE when ramUpdate is DDR4.");
            }
        }
    }

    private void validateGpuLaptopRule(DeviceType device, UpgradeGraphicCard gpu,
                                       List<String> errors) {
        if (device == DeviceType.LAPTOP && gpu != UpgradeGraphicCard.NONE) {
            errors.add("upgradeGraphicCard must be NONE for LAPTOP devices — " +
                    "discrete GPU upgrades are not supported on laptops.");
        }
    }

    private void validateAtLeastOneUpgrade(CpuUpdate cpu, RamUpdate ram,
                                           UpgradeGraphicCard gpu,
                                           List<String> errors) {
        boolean noCpu = cpu == CpuUpdate.NONE;
        boolean noRam = ram == RamUpdate.NONE;
        boolean noGpu = gpu == UpgradeGraphicCard.NONE;
        if (noCpu && noRam && noGpu) {
            errors.add("At least one upgrade must be selected: CPU, RAM, or GPU.");
        }
    }

    private void validateBookingDate(java.time.LocalDateTime date, List<String> errors) {
        if (date != null && date.isBefore(LocalDateTime.now())) {
            errors.add("bookingDate must be in the future.");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void requireNotNull(Object value, String fieldName, List<String> errors) {
        if (value == null) {
            errors.add(fieldName + " must not be null.");
        }
    }

    private void failIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            String detail = String.join(" | ", errors);
            ExceptionHandlerReporter.setResolveIssueDetails(detail);
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException(
                    "Validation failed for PerformanceCarePlan request: " + detail);
        }
    }
}
