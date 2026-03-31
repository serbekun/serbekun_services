package com.serbekun.service.auth.api;

/**
 * Read only interface for {@link com.serbekun.service.auth.EndpointRegistry}
 */
public interface EndpointAuthProvider {

    /**
     * 
     * return requires Auth
     * 
     * @return true if required, false if not
     */
    boolean requiresAuth(Endpoint endpoint);
}
