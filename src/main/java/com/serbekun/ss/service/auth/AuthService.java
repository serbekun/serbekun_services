package com.serbekun.ss.service.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointAuthProvider;
import com.serbekun.ss.service.tokens.EndpointsAccessTokensService;

/**
 * class that check need the endpoint token in request and check that
 */
public class AuthService {
    
    /** Logger for logging authentication-related events. */
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Service for managing endpoint access tokens. */
    private EndpointsAccessTokensService tokensService;
    /** Provider for checking endpoint authentication requirements. */
    private EndpointAuthProvider endpointAuthProvider;

    public AuthService(EndpointsAccessTokensService tokensService, EndpointAuthProvider endpointAuthProvider) {
        this.tokensService = tokensService;
        this.endpointAuthProvider = endpointAuthProvider;
    }

    /**
     * 
     * Auth request to server
     * 
     * @param endpoint endpoint value object
     * @param token request token
     * @return true Auth successfully. false Auth not successfully
    */
    public boolean checkAuth(Endpoint endpoint, String tokenHeader) {

        if (!endpointAuthProvider.requiresAuth(endpoint)) {
            log.debug("Auth not required for endpoint: {}", endpoint);
            return true;
        }

        if (tokenHeader == null || tokenHeader.isBlank()) {
            log.warn("Auth required for endpoint {} but no token provided", endpoint);
            return false;
        }

        // Accept both "Bearer <token>" and raw token
        String token = tokenHeader.startsWith("Bearer ")
            ? tokenHeader.substring("Bearer ".length()).trim()
            : tokenHeader.trim();

        if (token.isEmpty()) {
            log.warn("Auth required for endpoint {} but token is empty after stripping", endpoint);
            return false;
        }

        List<Endpoint> allowed = tokensService.getToken(token);

        boolean success = allowed != null && allowed.contains(endpoint);
        if (success) {
            log.debug("Auth successful for endpoint: {}", endpoint);
        } else {
            log.warn("Auth failed for endpoint {}: token does not grant access", endpoint);
        }
        return success;
    }
}
