package com.serbekun.ss.service.tokens;

import java.util.List;
import java.util.Map;

import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.service.auth.api.Endpoint;

public class EndpointsAccessTokensService {
    
    /** Repository for managing endpoint access tokens */
    private final EndpointsAccessTokensRepo tokens;

    public EndpointsAccessTokensService(EndpointsAccessTokensRepo tokens) {
        this.tokens = tokens;
    }

    /**
     * Add token to catalog
     * @param token the token string
     * @param allowedEndpoints list of endpoints that the token allows access to
     */
    public synchronized void addToken(String token, List<Endpoint> allowedEndpoints) {
        if (token == null) {
            return;
        }
        tokens.addToken(token, allowedEndpoints);
    }

    /**
     * Get token from catalog
     * @param token the token string
     * @return list of allowed endpoints for the token or null if not found
     */
    public synchronized List<Endpoint> getToken(String token) {
        return tokens.getToken(token);
    }

    /**
     * Get all tokens and their allowed endpoints
     * @return a map of token strings to their corresponding list of allowed endpoints
     */
    public synchronized Map<String, List<Endpoint>> getAllTokens() {
        return tokens.getEndpointsTokensData();
    }

    /**
     * Update token in catalog
     * @param token the token string
     * @param newAllowedEndpoints list of new allowed endpoints
     */
    public synchronized void updateToken(String token, List<Endpoint> newAllowedEndpoints) {
        tokens.removeToken(token);
        tokens.addToken(token, newAllowedEndpoints);
    }

    /**
     * Remove token from catalog
     *
     * @param token the token string
     * @return list of allowed endpoints for the removed token or null
     */
    public synchronized List<Endpoint> removeToken(String token) {
        if (token == null) {
            return null;
        }
        return tokens.removeToken(token);
    }
}
