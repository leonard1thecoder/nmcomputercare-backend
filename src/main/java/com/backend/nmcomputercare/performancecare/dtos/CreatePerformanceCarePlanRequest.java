package com.backend.nmcomputercare.performancecare.dtos;

import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Inbound payload to create a new {@link com.backend.nmcomputercare.performancecare.entity.PerformanceCarePlan}.
 *
 * <p>Pairing rules enforced by the validator:
 * <ul>
 *   <li>If {@code cpuUpdate = INTEL}: {@code intelModel} must not be NONE; {@code amdModel} must be NONE.</li>
 *   <li>If {@code cpuUpdate = AMD}:   {@code amdModel} must not be NONE; {@code intelModel} must be NONE.</li>
 *   <li>If {@code cpuUpdate = NONE}:  both model fields must be NONE.</li>
 *   <li>Similarly for RAM: only the matching DDR enum must be non-NONE.</li>
 *   <li>If {@code deviceType = LAPTOP}: {@code upgradeGraphicCard} must be NONE.</li>
 * </ul>
 */
@Data
@Builder
public class CreatePerformanceCarePlanRequest implements RequestContract {

    // CPU
    private CpuUpdate    cpuUpdate;
    private IntelModel   intelModel;
    private AmdModel     amdModel;

    // RAM
    private RamUpdate ramUpdate;
    private Ddr2Ram   ddr2Ram;
    private Ddr3Ram   ddr3Ram;
    private Ddr4Ram   ddr4Ram;

    // Device & GPU
    private DeviceType          deviceType;
    private UpgradeGraphicCard  upgradeGraphicCard;

    /** Optional: preferred appointment date/time requested by the client. */
    private LocalDateTime bookingDate;
}
