package com.varnild.scimApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class JsonResourceLoader {
	
	private ResourceLoader resourceLoader;

    @Autowired
    public JsonResourceLoader (ResourceLoader resourceLoader) {
    	this.resourceLoader = resourceLoader;
    }

    public Resource loadJsonResource(String location) {
        return resourceLoader.getResource(location);
    }
}
