package com.serbekun.service.auth;

import java.util.List;
import com.serbekun.service.tokens.TokensService;


// TODO add slf4j logger

/**
 * class that check need the endpoint token in request and check that
 */
public class AuthService {
    
    private TokensService tokensService;

    public AuthService(TokensService tokensService) {
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
    public boolean checkAuth(Endpoints endpoint, String token) {

        if (!endpoint.requiresAuth()) {
            return true;
        }

        List<Endpoints> allowed = tokensService.getToken(token);

        return allowed != null && allowed.contains(endpoint);
    }
}
