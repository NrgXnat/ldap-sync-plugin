package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class LdapXnatException extends Exception {
    public LdapXnatException(final String message) {
        super(message);
    }

    public LdapXnatException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LdapXnatException(final Throwable cause) {
        super(cause);
    }
}
