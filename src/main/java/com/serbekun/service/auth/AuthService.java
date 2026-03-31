package com.serbekun.service.auth;

import java.util.List;

import com.serbekun.service.auth.api.Endpoint;
import com.serbekun.service.auth.api.EndpointAuthProvider;
import com.serbekun.service.tokens.EndpointAccessTokensService;


// TODO add slf4j logger

/**
 * class that check need the endpoint token in request and check that
 */
public class AuthService {
    
    private EndpointAccessTokensService tokensService;
    private EndpointAuthProvider endpointAuthProvider;

    // public AuthService(EndpointAccessTokensService tokensService) {
    //     this(tokensService, endpoint -> true);
    // }

    public AuthService(EndpointAccessTokensService tokensService, EndpointAuthProvider endpointAuthProvider) {
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

        List<Endpoint> allowed = tokensService.getToken(token);

        return allowed != null && allowed.contains(endpoint);
    }
}
