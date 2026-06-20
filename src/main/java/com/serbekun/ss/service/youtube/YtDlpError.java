package com.serbekun.ss.service.youtube;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class YtDlpError {

    private final String error;

    @JsonCreator
    public YtDlpError(
            @JsonProperty("error") String error
        ) {
            this.error = error;
        }

        @JsonProperty("error")
        public String uuid() { return error; }

        @Override public boolean equals(Object o) {
            return (o instanceof YtDlpError) && error.equals(((YtDlpError) o).error);
        }
}