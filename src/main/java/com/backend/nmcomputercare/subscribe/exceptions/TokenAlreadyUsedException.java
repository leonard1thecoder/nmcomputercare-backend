package com.backend.nmcomputercare.subscribe.exceptions;

// ── Token already consumed ─────────────────────────────────────────────────────
public class TokenAlreadyUsedException extends RuntimeException {
    public TokenAlreadyUsedException(String message) { super(message); }
}
