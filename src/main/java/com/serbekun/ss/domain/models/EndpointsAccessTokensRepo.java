package com.serbekun.ss.domain.models;

import java.util.List;
import java.util.Map;

import com.serbekun.ss.service.auth.api.Endpoint;

/**
 * Holds endpoint-level access tokens and their allowed endpoints.
 */
public class EndpointsAccessTokensRepo implements EndpointsAccessTokensReadInterface {

    private final Map<String, List<Endpoint>> tokens;

    public EndpointsAccessTokensRepo(Map<String, List<Endpoint>> tokens) {
        this.tokens = tokens;
    }

    /**
     *
     * Add token to endpoint access catalog.
     *
     * @param token the token string
     * @param allowedEndpoints list of allowed endpoints
     */
    public synchronized void addToken(String token, List<Endpoint> allowedEndpoints) {
        if (token == null) {
            return;
        }
        tokens.put(token, allowedEndpoints);
    }

    /**
     *
     * Get allowed endpoints for token.
     *
     * @param token the token string
     * @return list of allowed endpoints or null
     */
    public synchronized List<Endpoint> getToken(String token) {
        return (token == null) ? null : tokens.get(token);
    }

    /**
     * Return all tokens.
     *
     * @return immutable copy of tokens map
     */
    public synchronized Map<String, List<Endpoint>> getEndpointsTokensData() {
        return Map.copyOf(tokens);
    }

    /**
     *
     * Helper method to update token.
     *
     * @param token the token string
     * @param newAllowedEndpoints new list of allowed endpoints
     */
    public synchronized void updateToken(String token, List<Endpoint> newAllowedEndpoints) {
        removeToken(token);
        addToken(token, newAllowedEndpoints);
    }

    /**
     *
     * Remove token from catalog.
     *
     * @param token the token string
     * @return the previous list of allowed endpoints or null
     */
    public synchronized List<Endpoint> removeToken(String token) {
        if (token == null) {
            return null;
        }
        return tokens.remove(token);
    }
}
