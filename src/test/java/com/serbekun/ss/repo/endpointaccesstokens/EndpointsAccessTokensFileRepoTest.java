package com.serbekun.ss.repo.endpointaccesstokens;

import com.serbekun.ss.service.auth.api.Endpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointsAccessTokensFileRepoTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyMapWhenFileMissing() {
        EndpointsAccessTokensFileRepo fileRepo =
            new EndpointsAccessTokensFileRepo(tempDir.resolve("missing.json"));

        assertThat(fileRepo.load()).isEmpty();
    }

    @Test
    void loadReturnsEmptyMapOnCorruptFile() throws IOException {
        Path file = tempDir.resolve("tokens.json");
        Files.writeString(file, "broken");

        assertThat(new EndpointsAccessTokensFileRepo(file).load()).isEmpty();
    }

    @Test
    void saveAndLoadRoundtrip() {
        Path file = tempDir.resolve("tokens.json");
        EndpointsAccessTokensRepo repo = new EndpointsAccessTokensRepo(new HashMap<>());
        repo.addToken("token-1", List.of(new Endpoint("/api/v0/version"), new Endpoint("/api/v0/cipher")));

        EndpointsAccessTokensFileRepo fileRepo = new EndpointsAccessTokensFileRepo(file);
        fileRepo.setEndpointsAccessTokensFileRepository(repo);
        fileRepo.save();

        Map<String, List<Endpoint>> loaded = new EndpointsAccessTokensFileRepo(file).load();
        assertThat(loaded).containsKey("token-1");
        assertThat(loaded.get("token-1"))
            .containsExactly(new Endpoint("/api/v0/version"), new Endpoint("/api/v0/cipher"));
    }
}
