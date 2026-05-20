package com.serbekun.ss.service.auth.api;

/**
 * Write only interface for {@link com.serbekun.ss.service.auth.EndpointRegistry}
 */
public interface EndpointRegistrar {

    /**
     * 
     * register new endpoint for auth can be check endpoint need auth or not
     * 
     * @param endpoint endpoint name
     * @param requiresAuth true endpoint requires auth false not requires auth
     */
    void register(Endpoint endpoint, boolean requiresAuth);

}
