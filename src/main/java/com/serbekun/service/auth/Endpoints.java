package com.serbekun.service.auth;

/**
 * Enum representing permission endpoints within the service.
 * Each constant indicates whether authentication is required.
 */
public enum Endpoints {

    /**
     * Permission endpoint for Links. Does not require authentication.
     */
    LINKS(false);

    /**
     * Indicates whether this endpoint requires authentication.
     */
    private final boolean requiresAuth;
    
    Endpoints(boolean requiresAuth) {
        this.requiresAuth = requiresAuth;
    }

    /**
     * 
     * return requires Auth
     * 
     * @return true if required, false if not
     */
    public boolean requiresAuth() {
        return requiresAuth;
    }
}