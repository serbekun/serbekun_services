package com.serbekun.ss.domain.models;

import java.util.List;
import java.util.Map;

import com.serbekun.ss.service.auth.api.Endpoint;

/**
 * Interface for provide to {@link com.serbekun.ss.repository.LinksFileRepo}
 * File Repository class only read access
 */
public interface EndpointsAccessTokensReadInterface {
    /**
     * @return Return all endpoint token data
     */
    Map<String, List<Endpoint>> getEndpointsTokensData();
    
}