package com.backend.nmcomputercare.subscribe.exceptions;

// ── Token not found ────────────────────────────────────────────────────────────
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) { super(message); }
}
