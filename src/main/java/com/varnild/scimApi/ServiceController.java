package com.varnild.scimApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/service")
public class ServiceController {
	
	@Autowired
	ScimService service;

	@GetMapping(path="/updateStoreStructure", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String updateDataStoreAndStructure(@RequestParam(required = true) String dFileName, @RequestParam(required = true) String sFileName) throws Exception {
		service.setDataStoreFileName(dFileName);
		service.setStructureFileName(sFileName);
		return "Data store and structure updated";
	}
	
	@GetMapping(path="/updateStore", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String updateDataStore(@RequestParam(required = true) String fileName) throws Exception {
		service.setDataStoreFileName(fileName);
		return "Data store and structure updated";
	}
	
	@GetMapping(path="/updateStructure", produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String updateStructure(@RequestParam(required = true) String fileName) throws Exception {
		service.setStructureFileName(fileName);
		return "Data store and structure updated";
	}

}
