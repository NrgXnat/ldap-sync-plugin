package com.radiologics.plugins.ldapsync.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Slf4j
public class SynchronizationExceptionTest {
    @Test
    public void testSynchronizationException() {
        String message = SynchronizationExceptionTest.class.toString();
        assertThatThrownBy(() -> { throw new SynchronizationException(message); }).hasMessage(message);

        Throwable throwable = new Exception("dummy");
        assertThatThrownBy(() -> { throw new SynchronizationException(throwable); }).hasMessage(throwable.toString());

        assertThatThrownBy(() -> {
            throw new SynchronizationException(message, throwable);
        }).hasMessage(message).getCause().hasMessageContaining("dummy");
    }
}
