package com.serbekun.service.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.serbekun.core.Programs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgramsJson {
    
    private final Programs programs;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = 
        LoggerFactory.getLogger(ProgramsJson.class);


    public ProgramsJson(Programs programs) {
        this.programs = programs;
    }

    /**
     * 
     * return json string from programs
     * 
     * @return json string of programs list
     */
    public synchronized String getProgramsListJson() {
        try {
            return mapper.writeValueAsString(programs.getAllPrograms());
        } catch (IOException e) {
            log.error("error make json string of programs list: {}", e);
            return null;
        }
    }
}
