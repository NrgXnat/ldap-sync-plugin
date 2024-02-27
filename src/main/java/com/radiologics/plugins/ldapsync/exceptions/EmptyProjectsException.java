package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmptyProjectsException extends Exception {
    public EmptyProjectsException(final String message) {
        super(message);
    }

    public EmptyProjectsException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EmptyProjectsException(final Throwable cause) {
        super(cause);
    }
}
