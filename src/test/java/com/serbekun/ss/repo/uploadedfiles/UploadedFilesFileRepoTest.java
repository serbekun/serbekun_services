package com.serbekun.ss.repo.uploadedfiles;

import com.serbekun.ss.domain.models.UploadedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UploadedFilesFileRepoTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyMapWhenFileMissingOrEmpty() throws IOException {
        assertThat(new UploadedFilesFileRepo(tempDir.resolve("missing.json")).load()).isEmpty();

        Path empty = tempDir.resolve("empty.json");
        Files.createFile(empty);
        assertThat(new UploadedFilesFileRepo(empty).load()).isEmpty();
    }

    @Test
    void loadReturnsEmptyMapOnCorruptFile() throws IOException {
        Path file = tempDir.resolve("files.json");
        Files.writeString(file, "###");

        assertThat(new UploadedFilesFileRepo(file).load()).isEmpty();
    }

    @Test
    void saveAndLoadRoundtrip() {
        Path file = tempDir.resolve("nested/files.json");
        UUID uuid = UUID.randomUUID();
        UploadedFilesRepo repo = new UploadedFilesRepo(new HashMap<>());
        repo.addUploadedFile(new UploadedFile(uuid, "doc.pdf", "token-1", 777L));

        UploadedFilesFileRepo fileRepo = new UploadedFilesFileRepo(file);
        fileRepo.setUploadedFilesReadInterface(repo);
        fileRepo.save();

        Map<UUID, UploadedFile> loaded = new UploadedFilesFileRepo(file).load();
        assertThat(loaded).containsKey(uuid);
        assertThat(loaded.get(uuid).name()).isEqualTo("doc.pdf");
        assertThat(loaded.get(uuid).token()).isEqualTo("token-1");
        assertThat(loaded.get(uuid).expiredTime()).isEqualTo(777L);
    }
}
