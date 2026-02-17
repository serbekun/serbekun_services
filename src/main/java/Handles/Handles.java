package Handles;

import io.javalin.Javalin;

public class Handles {
    
    /**
     * Init server api and resources endpoints 
    */
    public static void InitHandles(Javalin svr) {
        Index index = new Index();

        svr.get("/", index::Main);

    }

}
