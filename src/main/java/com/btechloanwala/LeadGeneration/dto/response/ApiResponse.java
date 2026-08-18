package com.btechloanwala.LeadGeneration.dto.response;

/**
 * Single reusable response envelope for every public endpoint.
 *
 * <p>Always serialized as {@code {"ok": ..., "message": ...}}. Success and error
 * responses both use this class so the React frontend has one stable contract.
 * Jackson derives the {@code ok} JSON key from the {@code isOk()} getter.</p>
 */
public class ApiResponse {

    private final boolean ok;
    private final String message;

    public ApiResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }
}