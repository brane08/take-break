package com.github.brane08.fx.takebreak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationLockTest {

    @AfterEach
    void releaseLock() {
        ApplicationLock.releaseLock();
    }

    @Test
    void tryToGetLockSucceedsWhenFree() {
        assertDoesNotThrow(ApplicationLock::tryToGetLock);
    }

    @Test
    void secondTryToGetLockWhileHeldThrows() {
        ApplicationLock.tryToGetLock();
        assertThrows(RuntimeException.class, ApplicationLock::tryToGetLock);
    }

    @Test
    void releaseLockFreesPortForSubsequentLock() {
        ApplicationLock.tryToGetLock();
        ApplicationLock.releaseLock();
        assertDoesNotThrow(ApplicationLock::tryToGetLock);
    }

    @Test
    void releaseLockIsIdempotent() {
        ApplicationLock.tryToGetLock();
        ApplicationLock.releaseLock();
        assertDoesNotThrow(ApplicationLock::releaseLock);
    }
}
