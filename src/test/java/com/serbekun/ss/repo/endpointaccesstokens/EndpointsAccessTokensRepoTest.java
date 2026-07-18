package com.serbekun.ss.repo.endpointaccesstokens;

import com.serbekun.ss.service.auth.api.Endpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointsAccessTokensRepoTest {

    private EndpointsAccessTokensRepo repo;

    @BeforeEach
    void setUp() {
        repo = new EndpointsAccessTokensRepo(new HashMap<>());
    }

    @Test
    void addAndGetToken() {
        List<Endpoint> endpoints = List.of(new Endpoint("/a"), new Endpoint("/b"));
        repo.addToken("token-1", endpoints);

        assertThat(repo.getToken("token-1")).containsExactlyElementsOf(endpoints);
    }

    @Test
    void nullTokenIsIgnored() {
        repo.addToken(null, List.of(new Endpoint("/a")));

        assertThat(repo.getEndpointsTokensData()).isEmpty();
        assertThat(repo.getToken(null)).isNull();
        assertThat(repo.removeToken(null)).isNull();
    }

    @Test
    void updateTokenReplacesEndpoints() {
        repo.addToken("token-1", List.of(new Endpoint("/a")));
        repo.updateToken("token-1", List.of(new Endpoint("/b")));

        assertThat(repo.getToken("token-1")).containsExactly(new Endpoint("/b"));
    }

    @Test
    void removeTokenReturnsPreviousValue() {
        List<Endpoint> endpoints = List.of(new Endpoint("/a"));
        repo.addToken("token-1", endpoints);

        assertThat(repo.removeToken("token-1")).isEqualTo(endpoints);
        assertThat(repo.getToken("token-1")).isNull();
    }

    @Test
    void getEndpointsTokensDataReturnsSnapshotCopy() {
        repo.addToken("token-1", List.of(new Endpoint("/a")));
        var snapshot = repo.getEndpointsTokensData();
        repo.addToken("token-2", List.of(new Endpoint("/b")));

        assertThat(snapshot).hasSize(1);
        assertThat(repo.getEndpointsTokensData()).hasSize(2);
    }
}
