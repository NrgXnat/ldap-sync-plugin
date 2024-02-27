package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SynchronizationException extends Exception {
    public SynchronizationException(final String message) {
        super(message);
    }

    public SynchronizationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SynchronizationException(final Throwable cause) {
        super(cause);
    }
}
