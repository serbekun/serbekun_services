package com.serbekun.core;

import java.util.Map;

/**
 * Core class that manage programs in memory
 */
public class Programs {
    
    private Map<String, program> programs; 

    public static class program {

        public String name;
        public String description;
        public int size; // size in bytes

        public program() {}
    }

    public Programs(Map<String, program> programs) {
        this.programs = programs;
    }

    /***
     * 
     * return all {@link Programs.program}
     * 
     * @return immutable copy of {@link Programs#programs}
     */
    public Map<String, program> getAllPrograms() {
        return Map.copyOf(programs);
    }

    /**
     * Add new program
     */
    // TODO 




}
