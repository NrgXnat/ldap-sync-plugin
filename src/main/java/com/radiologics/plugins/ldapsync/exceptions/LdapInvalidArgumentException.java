package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LdapInvalidArgumentException extends Exception {
    public LdapInvalidArgumentException(final String message) {
        super(message);
    }

    public LdapInvalidArgumentException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LdapInvalidArgumentException(final Throwable cause) {
        super(cause);
    }
}
