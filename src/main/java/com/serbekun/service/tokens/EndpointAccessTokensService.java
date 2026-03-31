package com.serbekun.service.tokens;

import java.util.List;
import java.util.Map;
import com.serbekun.core.EndpointsAccessTokens;
import com.serbekun.service.auth.api.Endpoint;

public class EndpointAccessTokensService {
    
    private final EndpointsAccessTokens tokens;

    public EndpointAccessTokensService(EndpointsAccessTokens tokens) {
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
        return tokens.getAllTokens();
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
