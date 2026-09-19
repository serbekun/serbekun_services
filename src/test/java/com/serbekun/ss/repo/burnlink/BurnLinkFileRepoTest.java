package com.serbekun.ss.repo.burnlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.serbekun.ss.domain.models.BurnLink;

class BurnLinkFileRepoTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyMapWhenFileMissingOrEmpty() throws IOException {
        assertThat(new BurnLinkFileRepo(tempDir.resolve("missing.json")).load()).isEmpty();

        Path empty = tempDir.resolve("empty.json");
        Files.createFile(empty);
        assertThat(new BurnLinkFileRepo(empty).load()).isEmpty();
    }

    @Test
    void loadReturnsEmptyMapOnCorruptFile() throws IOException {
        Path file = tempDir.resolve("burn.json");
        Files.writeString(file, "###");

        assertThat(new BurnLinkFileRepo(file).load()).isEmpty();
    }

    @Test
    void saveAndLoadRoundtrip() {
        Path file = tempDir.resolve("nested/burn.json");
        BurnLinkRepo repo = new BurnLinkRepo(new HashMap<>());
        repo.addBurnLink(new BurnLink("abc1234567", "top secret", "token-1", 111L, 777L,
                List.of("iphone"), List.of("firefox"), List.of("203.0.113.0/24"), List.of()));

        BurnLinkFileRepo fileRepo = new BurnLinkFileRepo(file);
        fileRepo.setBurnLinkReadInterface(repo);
        fileRepo.save();

        Map<String, BurnLink> loaded = new BurnLinkFileRepo(file).load();
        assertThat(loaded).containsKey("abc1234567");
        BurnLink link = loaded.get("abc1234567");
        assertThat(link.text()).isEqualTo("top secret");
        assertThat(link.token()).isEqualTo("token-1");
        assertThat(link.expiredTime()).isEqualTo(777L);
        assertThat(link.devices()).containsExactly("iphone");
        assertThat(link.browsers()).containsExactly("firefox");
        assertThat(link.ipWhitelist()).containsExactly("203.0.113.0/24");
    }
}
