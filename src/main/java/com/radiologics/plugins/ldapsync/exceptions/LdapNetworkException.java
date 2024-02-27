package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class LdapNetworkException extends Exception {
    public LdapNetworkException(final String message) {
        super(message);
    }

    public LdapNetworkException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LdapNetworkException(final Throwable cause) {
        super(cause);
    }
}
