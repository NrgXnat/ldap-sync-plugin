package com.radiologics.plugins.ldapsync.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class LdapXnatExceptionTest {
    @Test
    public void testLdapXnatException() {
        String message = LdapXnatExceptionTest.class.toString();
        assertThatThrownBy(() -> { throw new LdapXnatException(message); }).hasMessage(message);

        Throwable throwable = new Exception("dummy");
        assertThatThrownBy(() -> { throw new LdapXnatException(throwable); }).hasMessage(throwable.toString());

        assertThatThrownBy(() -> {
            throw new LdapXnatException(message, throwable);
        }).hasMessage(message).getCause().hasMessageContaining("dummy");
    }
}
