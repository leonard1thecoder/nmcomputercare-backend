package com.backend.nmcomputercare.subscribe.exceptions;

// ── Token window has passed ────────────────────────────────────────────────────
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) { super(message); }
}
