package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class WorkflowException extends Exception {
    public WorkflowException(final String message) {
        super(message);
    }

    public WorkflowException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WorkflowException(final Throwable cause) {
        super(cause);
    }
}
