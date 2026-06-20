package com.serbekun.ss.service.tokens;

import java.util.List;
import java.util.Map;

import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.service.auth.api.Endpoint;

public class EndpointsAccessTokensService {
    
    private final EndpointsAccessTokensRepo tokens;

    public EndpointsAccessTokensService(EndpointsAccessTokensRepo tokens) {
        this.tokens = tokens;
    }

    /**
     *
     * Add token to catalog
     *
     * @param token the token string
     * @param allowedEndpoints list of allowed endpoints
     */
    public synchronized void addToken(String token, List<Endpoint> allowedEndpoints) {
        if (token == null) {
            return;
        }
        tokens.addToken(token, allowedEndpoints);
    }

    /**
     *
     * get allowed endpoints for token
     *
     * @param token the token string
     * @return list of allowed endpoints or null
     */
    public synchronized List<Endpoint> getToken(String token) {
        return tokens.getToken(token);
    }

    /**
     * return all tokens
     *
     * @return immutable copy of tokens map
     */
    public synchronized Map<String, List<Endpoint>> getAllTokens() {
        return tokens.getEndpointsTokensData();
    }

    /**
     *
     * helper method for update token
     *
     * @param token the token string
     * @param newAllowedEndpoints new list of allowed endpoints
     */
    public synchronized void updateToken(String token, List<Endpoint> newAllowedEndpoints) {
        tokens.removeToken(token);
        tokens.addToken(token, newAllowedEndpoints);
    }

    /**
     *
     * remove token from catalog
     *
     * @param token the token string
     * @return the previous list of allowed endpoints or null
     */
    public synchronized List<Endpoint> removeToken(String token) {
        if (token == null) {
            return null;
        }
        return tokens.removeToken(token);
    }
}
