package com.serbekun.ss.service.auth;

import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.service.auth.api.Endpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private EndpointsAccessTokensRepo tokensRepo;
    private EndpointRegistry registry;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        tokensRepo = new EndpointsAccessTokensRepo(new HashMap<>());
        registry = new EndpointRegistry();
        authService = new AuthService(tokensRepo, registry);
    }

    /** EndpointRegistry keeps a static map, so use unique endpoint names per test. */
    private Endpoint uniqueEndpoint() {
        return new Endpoint("/test/" + UUID.randomUUID());
    }

    @Test
    void allowsRequestWhenEndpointDoesNotRequireAuth() {
        Endpoint endpoint = uniqueEndpoint();
        registry.register(endpoint, false);

        assertThat(authService.checkAuth(endpoint, null)).isTrue();
        assertThat(authService.checkAuth(endpoint, "whatever")).isTrue();
    }

    @Test
    void unregisteredEndpointDefaultsToNoAuthRequired() {
        assertThat(authService.checkAuth(uniqueEndpoint(), null)).isTrue();
    }

    @Test
    void rejectsMissingOrBlankTokenWhenAuthRequired() {
        Endpoint endpoint = uniqueEndpoint();
        registry.register(endpoint, true);

        assertThat(authService.checkAuth(endpoint, null)).isFalse();
        assertThat(authService.checkAuth(endpoint, "   ")).isFalse();
        assertThat(authService.checkAuth(endpoint, "Bearer ")).isFalse();
    }

    @Test
    void acceptsValidRawToken() {
        Endpoint endpoint = uniqueEndpoint();
        registry.register(endpoint, true);
        tokensRepo.addToken("valid-token", List.of(endpoint));

        assertThat(authService.checkAuth(endpoint, "valid-token")).isTrue();
    }

    @Test
    void acceptsValidBearerToken() {
        Endpoint endpoint = uniqueEndpoint();
        registry.register(endpoint, true);
        tokensRepo.addToken("valid-token", List.of(endpoint));

        assertThat(authService.checkAuth(endpoint, "Bearer valid-token")).isTrue();
    }

    @Test
    void rejectsUnknownToken() {
        Endpoint endpoint = uniqueEndpoint();
        registry.register(endpoint, true);

        assertThat(authService.checkAuth(endpoint, "unknown-token")).isFalse();
    }

    @Test
    void rejectsTokenThatDoesNotGrantThisEndpoint() {
        Endpoint protectedEndpoint = uniqueEndpoint();
        Endpoint otherEndpoint = uniqueEndpoint();
        registry.register(protectedEndpoint, true);
        tokensRepo.addToken("other-token", List.of(otherEndpoint));

        assertThat(authService.checkAuth(protectedEndpoint, "other-token")).isFalse();
    }

    // region Endpoint value object

    @Test
    void endpointRejectsNullOrBlankName() {
        assertThatThrownBy(() -> new Endpoint(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Endpoint("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void endpointEqualityIsByName() {
        assertThat(new Endpoint("/a")).isEqualTo(new Endpoint("/a"));
        assertThat(new Endpoint("/a")).isNotEqualTo(new Endpoint("/b"));
        assertThat(new Endpoint("/a").hashCode()).isEqualTo(new Endpoint("/a").hashCode());
    }

    // endregion

    // region EndpointRegistry

    @Test
    void registryStoresAuthRequirement() {
        Endpoint secured = uniqueEndpoint();
        Endpoint open = uniqueEndpoint();
        registry.register(secured, true);
        registry.register(open, false);

        assertThat(registry.requiresAuth(secured)).isTrue();
        assertThat(registry.requiresAuth(open)).isFalse();
        assertThat(registry.requiresAuth(uniqueEndpoint())).isFalse();
    }

    // endregion
}
