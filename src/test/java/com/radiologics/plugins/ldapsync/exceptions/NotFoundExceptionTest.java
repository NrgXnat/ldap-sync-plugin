package com.radiologics.plugins.ldapsync.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class NotFoundExceptionTest {
    @Test
    public void testNotFoundException() {
        String message = NotFoundExceptionTest.class.toString();
        assertThatThrownBy(() -> { throw new NotFoundException(message); }).hasMessage(message);

        Throwable throwable = new Exception("dummy");
        assertThatThrownBy(() -> { throw new NotFoundException(throwable); }).hasMessage(throwable.toString());

        assertThatThrownBy(() -> {
            throw new NotFoundException(message, throwable);
        }).hasMessage(message).getCause().hasMessageContaining("dummy");
    }
}
