package com.radiologics.plugins.ldapsync.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GroupDNNotFoundException extends NotFoundException {
    public GroupDNNotFoundException(final String message) {
        super(message);
    }

    public GroupDNNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public GroupDNNotFoundException(final Throwable cause) {
        super(cause);
    }
}
