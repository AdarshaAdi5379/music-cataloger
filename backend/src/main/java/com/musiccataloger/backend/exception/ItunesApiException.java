package com.musiccataloger.backend.exception;

/**
 * Thrown when the iTunes Search API call fails for any reason:
 * network error, timeout, unexpected response, or HTTP error status.
 *
 * The GlobalExceptionHandler maps this to HTTP 502 Bad Gateway
 * with a generic user-facing message — internal details are never surfaced.
 */
public class ItunesApiException extends RuntimeException {

    public ItunesApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItunesApiException(String message) {
        super(message);
    }
}
