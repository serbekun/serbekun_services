package com.serbekun.ss.repo.localtokens;

import java.util.Map;

/**
* Interface for provide to {@link com.serbekun.ss.repo.localtokens.repository.repo.localtokens.LocalTokensFileRepo}
* File Repository class only read access
*/
public interface LocalTokensReadInterface {
    /**
     * @return All tokens data.
     */
    Map<String, String> getTokensData();
}
