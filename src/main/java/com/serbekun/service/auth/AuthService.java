package com.serbekun.service.auth;

import java.util.List;
import com.serbekun.service.tokens.EndpointAccessTokensService;


// TODO add slf4j logger

/**
 * class that check need the endpoint token in request and check that
 */
public class AuthService {
    
    private EndpointAccessTokensService tokensService;

    public AuthService(EndpointAccessTokensService tokensService) {
        this.tokensService = tokensService;
    }

    /**
     * 
     * Auth request to server
     * 
     * @param endpoints endpoint enum
     * @param token request token
     * @return true Auth successfully. false Auth not successfully
    */
    public boolean checkAuth(Endpoints endpoint, String tokenHeader) {

        if (!endpoint.requiresAuth()) {
            return true;
        }

        if (tokenHeader == null || tokenHeader.isBlank()) {
            return false;
        }

        // Accept both "Bearer <token>" and raw token
        String token = tokenHeader.startsWith("Bearer ")
            ? tokenHeader.substring("Bearer ".length()).trim()
            : tokenHeader.trim();

        if (token.isEmpty()) {
            return false;
        }

        List<Endpoints> allowed = tokensService.getToken(token);

        return allowed != null && allowed.contains(endpoint);
    }
}
