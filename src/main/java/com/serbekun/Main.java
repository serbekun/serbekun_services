package com.serbekun;


import java.nio.file.Path;
import java.util.UUID;

import com.serbekun.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.config.core.CoreConfig;
import com.serbekun.core.Links;
import com.serbekun.core.Links.Link;
import com.serbekun.repository.LinksRepository;

public class Main {
    
    public static void main(String[] args) {
        
        // init server folders
        ServerStorageInitializer serverStorageInitializer = new ServerStorageInitializer();
        serverStorageInitializer.initialize(Path.of(CoreConfig.Infrastructure.Fs.getServerStorageFolder()));
        
        LinksRepository linksRepository = new LinksRepository(CoreConfig.LinksConfig.getLinksStorageFile());
        
        Links links = new Links(linksRepository.load()); 
        
        Link link1 = new Link(UUID.randomUUID(), "https://github.com", "github", "github index page");
        Link link2 = new Link(UUID.randomUUID(), "https://youtube.com", "youtube", "youtube index page");

        links.addLink(link1);
        links.addLink(link2);

        linksRepository.save(links.getAllLinks());

    }
}
