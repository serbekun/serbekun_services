package com.serbekun.ss.repo.uploadedfiles;

import com.serbekun.ss.domain.models.UploadedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UploadedFilesRepoTest {

    private UploadedFilesRepo repo;

    @BeforeEach
    void setUp() {
        repo = new UploadedFilesRepo(new HashMap<>());
    }

    @Test
    void addAndGetUploadedFile() {
        UploadedFile file = new UploadedFile(UUID.randomUUID(), "a.txt", "t", 0);
        repo.addUploadedFile(file);

        assertThat(repo.getUploadedFile(file.uuid())).isEqualTo(file);
        assertThat(repo.existsUploadedFile(file.uuid())).isTrue();
    }

    @Test
    void addIgnoresNull() {
        repo.addUploadedFile(null);

        assertThat(repo.getUploadedFilesData()).isEmpty();
    }

    @Test
    void getAndRemoveAreNullSafe() {
        assertThat(repo.getUploadedFile(null)).isNull();
        assertThat(repo.removeUploadedFile(null)).isNull();
    }

    @Test
    void updateReplacesEntry() {
        UUID uuid = UUID.randomUUID();
        repo.addUploadedFile(new UploadedFile(uuid, "old.txt", "t", 0));
        UploadedFile updated = new UploadedFile(uuid, "new.txt", "t2", 42);

        repo.updateUploadedFile(uuid, updated);

        assertThat(repo.getUploadedFile(uuid).name()).isEqualTo("new.txt");
        assertThat(repo.getUploadedFilesData()).hasSize(1);
    }

    @Test
    void removeReturnsRemovedValue() {
        UploadedFile file = new UploadedFile(UUID.randomUUID(), "a.txt", "t", 0);
        repo.addUploadedFile(file);

        assertThat(repo.removeUploadedFile(file.uuid())).isEqualTo(file);
        assertThat(repo.existsUploadedFile(file.uuid())).isFalse();
    }

    @Test
    void getUploadedFilesDataReturnsSnapshotCopy() {
        repo.addUploadedFile(new UploadedFile(UUID.randomUUID(), "a.txt", "t", 0));
        var snapshot = repo.getUploadedFilesData();
        repo.addUploadedFile(new UploadedFile(UUID.randomUUID(), "b.txt", "t", 0));

        assertThat(snapshot).hasSize(1);
        assertThat(repo.getUploadedFilesData()).hasSize(2);
    }
}
