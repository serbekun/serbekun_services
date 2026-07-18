package com.serbekun.ss.service.uploadedfiles;

import com.serbekun.ss.domain.models.UploadedFile;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadedFilesServiceTest {

    @TempDir
    Path tempDir;

    private Path rawDir;
    private UploadedFilesRepo repo;
    private UploadedFilesService service;

    @BeforeEach
    void setUp() {
        rawDir = tempDir.resolve("raw");
        repo = new UploadedFilesRepo(new HashMap<>());
        service = new UploadedFilesService(repo, rawDir);
    }

    private UploadedFile upload(String name, String token, long expiredTime, byte[] content) throws IOException {
        UploadedFile meta = new UploadedFile(UUID.randomUUID(), name, token, expiredTime);
        return service.uploadFile(meta, content);
    }

    // region uploadFile

    @Test
    void uploadFileWritesContentAndMetadata() throws IOException {
        byte[] content = "file content".getBytes(StandardCharsets.UTF_8);
        UploadedFile meta = upload("test.txt", "token-1", 0, content);

        assertThat(repo.getUploadedFile(meta.uuid())).isEqualTo(meta);
        assertThat(rawDir.resolve(meta.uuid().toString())).hasBinaryContent(content);
    }

    @Test
    void uploadFileRejectsNullMetadataOrContent() {
        UploadedFile meta = new UploadedFile(UUID.randomUUID(), "a", "t", 0);

        assertThatThrownBy(() -> service.uploadFile(null, new byte[0]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.uploadFile(meta, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // endregion

    // region verifyFileAccess

    @Test
    void verifyFileAccessRequiresMatchingToken() throws IOException {
        UploadedFile meta = upload("a.txt", "secret", 0, new byte[] {1});

        assertThat(service.verifyFileAccess(meta.uuid(), "secret")).isEqualTo(meta);
        assertThat(service.verifyFileAccess(meta.uuid(), "wrong")).isNull();
        assertThat(service.verifyFileAccess(meta.uuid(), null)).isNull();
        assertThat(service.verifyFileAccess(UUID.randomUUID(), "secret")).isNull();
    }

    // endregion

    // region getFileContent

    @Test
    void getFileContentReturnsStoredBytes() throws IOException {
        byte[] content = "payload".getBytes(StandardCharsets.UTF_8);
        UploadedFile meta = upload("a.txt", "t", 0, content);

        assertThat(service.getFileContent(meta.uuid())).isEqualTo(content);
    }

    @Test
    void getFileContentReturnsNullForUnknownUuid() throws IOException {
        assertThat(service.getFileContent(UUID.randomUUID())).isNull();
    }

    @Test
    void getFileContentReturnsNullWhenRawFileMissing() throws IOException {
        UploadedFile meta = new UploadedFile(UUID.randomUUID(), "a.txt", "t", 0);
        repo.addUploadedFile(meta); // metadata only, no bytes on disk

        assertThat(service.getFileContent(meta.uuid())).isNull();
    }

    @Test
    void getFileContentRemovesExpiredFile() throws IOException {
        long past = System.currentTimeMillis() - 1000;
        UploadedFile meta = upload("expired.txt", "t", past, new byte[] {1, 2, 3});

        assertThat(service.getFileContent(meta.uuid())).isNull();
        assertThat(repo.existsUploadedFile(meta.uuid())).isFalse();
        assertThat(Files.exists(rawDir.resolve(meta.uuid().toString()))).isFalse();
    }

    @Test
    void getFileContentKeepsFileWithZeroExpiredTime() throws IOException {
        // expiredTime == 0 means "never expires"
        UploadedFile meta = upload("forever.txt", "t", 0, new byte[] {7});

        assertThat(service.getFileContent(meta.uuid())).isNotNull();
        assertThat(repo.existsUploadedFile(meta.uuid())).isTrue();
    }

    // endregion

    // region deleteFile

    @Test
    void deleteFileRemovesContentAndMetadata() throws IOException {
        UploadedFile meta = upload("a.txt", "secret", 0, new byte[] {1});

        assertThat(service.deleteFile(meta.uuid(), "secret")).isEqualTo(204);
        assertThat(repo.existsUploadedFile(meta.uuid())).isFalse();
        assertThat(Files.exists(rawDir.resolve(meta.uuid().toString()))).isFalse();
    }

    @Test
    void deleteFileReturns403ForWrongOrMissingToken() throws IOException {
        UploadedFile meta = upload("a.txt", "secret", 0, new byte[] {1});

        assertThat(service.deleteFile(meta.uuid(), "wrong")).isEqualTo(403);
        assertThat(service.deleteFile(meta.uuid(), null)).isEqualTo(403);
        assertThat(repo.existsUploadedFile(meta.uuid())).isTrue();
    }

    @Test
    void deleteFileReturns404ForUnknownUuid() {
        assertThat(service.deleteFile(UUID.randomUUID(), "any")).isEqualTo(404);
    }

    // endregion

    // region deleteExpiredFiles

    @Test
    void deleteExpiredFilesRemovesOnlyExpiredEntries() throws IOException {
        long past = System.currentTimeMillis() - 1000;
        long future = System.currentTimeMillis() + 60_000;
        UploadedFile expired1 = upload("e1.txt", "t", past, new byte[] {1});
        UploadedFile expired2 = upload("e2.txt", "t", past, new byte[] {2});
        UploadedFile eternal = upload("eternal.txt", "t", 0, new byte[] {3});
        UploadedFile fresh = upload("fresh.txt", "t", future, new byte[] {4});

        int removed = service.deleteExpiredFiles();

        assertThat(removed).isEqualTo(2);
        assertThat(repo.existsUploadedFile(expired1.uuid())).isFalse();
        assertThat(repo.existsUploadedFile(expired2.uuid())).isFalse();
        assertThat(repo.existsUploadedFile(eternal.uuid())).isTrue();
        assertThat(repo.existsUploadedFile(fresh.uuid())).isTrue();
        assertThat(Files.exists(rawDir.resolve(expired1.uuid().toString()))).isFalse();
        assertThat(Files.exists(rawDir.resolve(fresh.uuid().toString()))).isTrue();
    }

    @Test
    void deleteExpiredFilesReturnsZeroWhenNothingExpired() throws IOException {
        upload("fresh.txt", "t", System.currentTimeMillis() + 60_000, new byte[] {1});

        assertThat(service.deleteExpiredFiles()).isZero();
    }

    // endregion
}
