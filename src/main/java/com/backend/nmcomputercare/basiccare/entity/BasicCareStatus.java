package com.backend.nmcomputercare.basiccare.entity;

/**
 * Lifecycle states of a {@link BasicCarePlan}.
 *
 * <pre>
 *  0 CREATED            – Plan saved; awaiting quote acceptance / payment
 *  1 MISSING_PAYMENT    – Quote accepted but payment has not been received
 *  2 QUOTE_REJECTED     – Client declined the quoted price
 *  3 PAID               – Payment confirmed; work may begin
 *  4 WAITING_APPOINTMENT – Paid; waiting for the client's appointment slot
 *  5 COMPLETED          – Service fully delivered
 * </pre>
 */
public enum BasicCareStatus {

    CREATED(0,            "Created",             "Plan created and awaiting client response."),
    MISSING_PAYMENT(1,    "Missing Payment",     "Quote accepted but payment has not been received."),
    QUOTE_REJECTED(2,     "Quote Rejected",      "Client declined the quoted price."),
    PAID(3,               "Paid",                "Payment confirmed. Work may begin."),
    WAITING_APPOINTMENT(4,"Waiting Appointment", "Paid and queued for a technician appointment."),
    COMPLETED(5,          "Completed",           "Service has been fully delivered.");

    private final byte   code;
    private final String displayName;
    private final String description;

    BasicCareStatus(int code, String displayName, String description) {
        this.code        = (byte) code;
        this.displayName = displayName;
        this.description = description;
    }

    public byte   getCode()        { return code;        }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /** Resolve a {@code BasicCareStatus} from its numeric code. */
    public static BasicCareStatus fromCode(byte code) {
        for (BasicCareStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown BasicCareStatus code: " + code);
    }
}
