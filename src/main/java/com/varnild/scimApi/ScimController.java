package com.varnild.scimApi;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/scim")
public class ScimController {
	
	@Autowired
	ScimService service;
	
	@GetMapping(path="/Groups", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String scimGroup(@RequestParam String filter) throws Exception {
		if(filter.equals("displayName co \"music\"")) {
			return service.getGroups();
		} else {
			throw new ResponseStatusException(
			          HttpStatus.NOT_FOUND, "Wrong filter, should be displayName co \\\"music\\\"", null);
		}
	}
	
	@GetMapping(path="/Users/", produces="application/scim+json")
	@ResponseBody
	public String getUser() throws Exception {
		return service.getUsers();
	}
	
	@GetMapping(path="/Users/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getUser(@PathVariable String id) throws Exception {
		return service.getUser(id, false);
	}

	@GetMapping(path="/Groups/", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getGroups() throws Exception {
		return service.getGroups();
	}
	
	@GetMapping(path="/Groups/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getGroup(@PathVariable String id) throws Exception {
		return service.getGroup(id, false);
	}
	
}
