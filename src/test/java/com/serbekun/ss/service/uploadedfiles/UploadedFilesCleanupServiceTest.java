package com.serbekun.ss.service.uploadedfiles;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Note: deleteExpiredFiles() is synchronized, so Mockito's verify(timeout(...))
 * cannot be used here — the verifying thread would hold the mock's monitor and
 * block the scheduler thread. CountDownLatch answers avoid that.
 */
class UploadedFilesCleanupServiceTest {

    @Test
    void periodicallyDeletesExpiredFiles() throws InterruptedException {
        UploadedFilesService service = mock(UploadedFilesService.class);
        CountDownLatch invoked = new CountDownLatch(1);
        when(service.deleteExpiredFiles()).thenAnswer(inv -> {
            invoked.countDown();
            return 0;
        });
        UploadedFilesCleanupService cleanup = new UploadedFilesCleanupService(service, 1);

        cleanup.start();
        try {
            assertThat(invoked.await(5, TimeUnit.SECONDS))
                .as("deleteExpiredFiles should be called within 5 seconds")
                .isTrue();
        } finally {
            cleanup.stop();
        }
    }

    @Test
    void cleanupSurvivesServiceExceptions() throws InterruptedException {
        UploadedFilesService service = mock(UploadedFilesService.class);
        CountDownLatch invokedTwice = new CountDownLatch(2);
        when(service.deleteExpiredFiles()).thenAnswer(inv -> {
            invokedTwice.countDown();
            throw new RuntimeException("boom");
        });
        UploadedFilesCleanupService cleanup = new UploadedFilesCleanupService(service, 1);

        cleanup.start();
        try {
            // If the exception killed the scheduler, a second invocation would never come
            assertThat(invokedTwice.await(5, TimeUnit.SECONDS))
                .as("cleanup should keep running after an exception")
                .isTrue();
        } finally {
            cleanup.stop();
        }
    }

    @Test
    void stopIsIdempotent() {
        UploadedFilesService service = mock(UploadedFilesService.class);
        UploadedFilesCleanupService cleanup = new UploadedFilesCleanupService(service, 1);

        cleanup.start();
        cleanup.stop();
        cleanup.stop(); // second stop must not throw
    }
}
