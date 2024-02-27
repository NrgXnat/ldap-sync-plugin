package com.radiologics.plugins.ldapsync.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class AlreadyExistsExceptionTest {
    @Test
    public void testAlreadyExistsException() {
        String message = AlreadyExistsExceptionTest.class.toString();
        assertThatThrownBy(() -> { throw new AlreadyExistsException(message); }).hasMessage(message);

        Throwable throwable = new Exception("dummy");
        assertThatThrownBy(() -> { throw new AlreadyExistsException(throwable); }).hasMessage(throwable.toString());

        assertThatThrownBy(() -> {
            throw new AlreadyExistsException(message, throwable);
        }).hasMessage(message).getCause().hasMessageContaining("dummy");
    }
}
