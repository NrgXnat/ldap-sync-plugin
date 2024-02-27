package com.radiologics.plugins.ldapsync.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class LdapNetworkExceptionTest {
    @Test
    public void testLdapNetworkException() {
        String message = LdapNetworkExceptionTest.class.toString();
        assertThatThrownBy(() -> { throw new LdapNetworkException(message); }).hasMessage(message);

        Throwable throwable = new Exception("dummy");
        assertThatThrownBy(() -> { throw new LdapNetworkException(throwable); }).hasMessage(throwable.toString());

        assertThatThrownBy(() -> {
            throw new LdapNetworkException(message, throwable);
        }).hasMessage(message).getCause().hasMessageContaining("dummy");
    }
}
