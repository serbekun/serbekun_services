package com.serbekun.ss.domain.dto.http;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic error payload shared by every API handler.
 * <p>
 * {@code message} is optional — it is omitted from the response when null, so
 * handlers that only need a single error string can use {@link #of(String)}.
 *
 * @param error   short error text or machine readable code
 * @param message optional human readable explanation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message) {

    /**
     * Creates an error payload without a message.
     * @param error short error text
     * @return the error payload
     */
    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }
}
