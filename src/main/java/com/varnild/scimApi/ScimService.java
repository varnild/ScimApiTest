package com.varnild.scimApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

@Service
public class ScimService {
	
	private String dataStoreFileName;
	private String structureFileName;

	private JsonNode structureNode;
	private JsonNode groupStructureNode;
	private JsonNode userStructureNode;
	private JsonNode rawDataNode;
	private JsonNode groupDataNodes;
	private JsonNode userDataNodes;
	private String serverUrl;
	private ObjectMapper objectMapper;
	
	private String header = "{\n"
			+ "    \"totalResults\": 5,\n"
			+ "    \"itemsPerPage\": 5,\n"
			+ "    \"startIndex\": 1,\n"
			+ "    \"schemas\": [\n"
			+ "        \"urn:ietf:params:scim:api:messages:2.0:ListResponse\"\n"
			+ "    ],\n"
			+ "    \"Resources\": [ %s  ]}";
	
	
	@PostConstruct
	public void init() throws Exception {
		dataStoreFileName = "rawData";
		structureFileName = "structure";
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		loadData();
	}
	
	public void loadData() throws Exception {
		try {
			// hard coded path to the main resource, with a fallback to current dir path
			String currentDir = System.getProperty("user.dir");
			String dataPath = currentDir + "/src/main/resources/scimApiTest/" + dataStoreFileName + ".json";
			File dataFile = new File(dataPath);
			
			if (!dataFile.exists()) { 
				dataFile = new File(currentDir + "/" + dataStoreFileName + ".json");
				System.out.println("Path used to fetch data file: " + currentDir + "/" + dataStoreFileName + ".json");
			} else {
				System.out.println("Path used to fetch data file: " + dataPath);
			}
			
			try (InputStream inputStreamRawData = new FileInputStream(dataFile)) {
				// data tree of groups and users
				rawDataNode = objectMapper.readTree(inputStreamRawData);
				// get the list of groups as data 
				groupDataNodes = rawDataNode.get("groups");
				serverUrl = rawDataNode.get("serverUrl").asText();
				userDataNodes = rawDataNode.get("users");
			} catch (Exception e) {
				throw new Exception("Something went wrong during the streaming of the json files", e);
			}
			
			String structurePath = currentDir + "/src/main/resources/scimApiTest/" + structureFileName + ".json";
			File structureFile = new File(structurePath);
			
			if (!structureFile.exists()) { 
				structureFile = new File(currentDir + "/" + structureFileName + ".json");
				System.out.println("Path used to fetch structure file: " + currentDir + "/" + structureFileName + ".json");
			} else {
				System.out.println("Path used to fetch structure file: " + structurePath);
			}
			
			try (InputStream inputStreamStructure = new FileInputStream(structureFile)) {
				structureNode = objectMapper.readTree(inputStreamStructure);
				// Node for a group structure that we'll use for cloning
				groupStructureNode = structureNode.get("group");
				// Node for a user structure that we'll use for cloning
				userStructureNode = structureNode.get("user");
			} catch (Exception e) {
				throw new Exception("Something went wrong during the streaming of the json files", e);
			}

		} catch (Exception e) {
			throw new Exception("Something went wrong during the loading of the json files, please verify that " + dataStoreFileName + ".json and " + structureFileName + ".json files exists at the same path as the ScimApiTest jar" , e);
		}
	}
	
	public void setDataStoreFileName(String dataFileName) {
		this.dataStoreFileName = dataFileName;
	}
	public void setStructureFileName(String structureFileName) {
		this.structureFileName = structureFileName;
	}

	public String getGroups() throws Exception {
		loadData();
		StringBuffer sb = new StringBuffer();
		int i = 0;
		for(JsonNode groupDataNode : groupDataNodes ) {
			sb.append(((i==0)? "" : ", ") + getGroup(groupDataNode.get("id").asText(), true));
			i++;
		}
		return String.format(header, sb.toString());
	}
	
	public String getGroup (String groupId, boolean alreadyLoaded) throws Exception {
		if(!alreadyLoaded) loadData();
		// node to store data related to the group where are fetching coming from rawData
		JsonNode groupData = objectMapper.createObjectNode();
		for(JsonNode groupDataNode : groupDataNodes ) {
			if(groupDataNode.get("id").asText().equals(groupId)) {
				groupData = groupDataNode;
				break;
			}
		}
		// clone the group structure node
		JsonNode structClonedNode = groupStructureNode.deepCopy();
    	// Iterator on the field on the group structure node
    	Iterator<Map.Entry<String, JsonNode>> stFields = ((ObjectNode) structClonedNode).fields();
    	while (stFields.hasNext()) {
    		Entry<String, JsonNode> nStField = stFields.next();
    		// if we are at the members field prepare the list of member node to be appended
    		if(nStField.getKey().equals("members")) {
    			JsonNodeFactory factory = JsonNodeFactory.instance;
    			// create an empty array for members node
    			ArrayNode clonedMembersNode = factory.arrayNode();
    			// get the structure of a members node
    			JsonNode structureMemberNode = nStField.getValue().get(0);
    			
    			// in the group members structure node replace the serverUrl placeholder by its value from rawData
    			if(structureMemberNode.has("$ref")) {
    				String ref = structureMemberNode.get("$ref").asText();
                    ref = ref.replace("{serverUrl}", serverUrl);
                    ((ObjectNode) structureMemberNode).put("$ref", ref);
    			}
                // now our group structure node is ready we will replace the placeholders with datas
                
                // for each member of the group (from rawData)
                for(JsonNode memberId : groupData.get("members")) {
                	// get the data for the interesting member and put it in memberData
                	JsonNode memberData = objectMapper.createObjectNode();
                	for(JsonNode userDataNode : userDataNodes ) {
            			if(userDataNode.get("id").asText().equals(memberId.get("id").asText())) {
            				memberData = userDataNode;
            				break;
            			}
            		}
                	// prepare the clone from the structure to fill with data
                	JsonNode structMemberClonedNode = structureMemberNode.deepCopy();
                	// iterator on the field of the group members structure
    				Iterator<Map.Entry<String, JsonNode>> memberFields = ((ObjectNode) structMemberClonedNode).fields();
                	while (memberFields.hasNext()) {
    					Entry<String, JsonNode> zMemberfield = memberFields.next();
    					// special case for email, we deal with both email and emails in the structure
    					if(zMemberfield.getKey().equals("email")
    							|| zMemberfield.getKey().equals("emails")) {
    						// from the data side only emails is allowed
    						try {
    							JsonNode emailList = memberData.get("emails");
    							String temp = "";
        						for(JsonNode email: emailList) {
        							// if there is only one email available use it, or if it is tagged as primary
        							if(email.has("primary") || emailList.size() == 1) {
        								temp = email.get("value").asText();
        								break;
        							}
        						}
        						// replace the node in the clone by the one we just created
        						((ObjectNode) structMemberClonedNode).put(zMemberfield.getKey(), temp);
    						} catch (Exception e) {
    							throw new Exception("An attribute emails is needed for user " + memberData.get("id") , e);
							}
    					} else {
    						//regular field of a member case
    						replacePlaceHolder(structMemberClonedNode, memberData, zMemberfield);
    					}
    				}
    				clonedMembersNode.add(structMemberClonedNode);
                }
                // replace the node in the clone by the list of members we created
    			((ObjectNode) structClonedNode).set("members", clonedMembersNode);
    		} else {
    			// regular field, let's find placeholders and replace them
    			replacePlaceHolder(structClonedNode, groupData, nStField);
    		}
    	}
		return objectMapper.writeValueAsString(structClonedNode);
	}
	
	public String getUsers() throws Exception {
		loadData();
		StringBuffer sb = new StringBuffer();
		int i = 0;
		for(JsonNode userDataNode : userDataNodes ) {
			sb.append(((i==0)? "" : ", " ) + getUser(userDataNode.get("id").asText(), true));
			i++;
		}
		return String.format(header, sb.toString());
	}
	
	public String getUser(String userId, boolean alreadyLoaded) throws Exception {
		if(!alreadyLoaded) loadData();
		
		// get the data for our user
		JsonNode userData = objectMapper.createObjectNode();
		for (JsonNode userDataNode : userDataNodes) {
			if(userDataNode.get("id").asText().equals(userId)) {
				userData = userDataNode;
				break;
			}
		}
		
		// clone the user structure node
		JsonNode structClonedNode = userStructureNode.deepCopy();
		// Iterator on the field on the group structure node
    	Iterator<Map.Entry<String, JsonNode>> stFields = ((ObjectNode) structClonedNode).fields();
    	while (stFields.hasNext()) {
    		Entry<String, JsonNode> nStField = stFields.next();
    		replacePlaceHolder(structClonedNode, userData, nStField);
    		if(nStField.getKey().equals("groups")) {
    			
    			JsonNodeFactory factory = JsonNodeFactory.instance;
    			// create an empty array for members node
    			ArrayNode clonedGroupsNode = factory.arrayNode();
    			// get the structure of a group node
    			JsonNode structureGroupNode = nStField.getValue().get(0);
    			
    			for (JsonNode groupDataNode : groupDataNodes) {
    				JsonNode membersNode = groupDataNode.get("members"); 
    				for(JsonNode memberNode: membersNode) {
    					JsonNode groupClone = structureGroupNode.deepCopy();
    					if(memberNode.get("id").asText().equals(userId)) {
    						Iterator<Map.Entry<String, JsonNode>> zFields = ((ObjectNode) structureGroupNode).fields();
    				    	while (zFields.hasNext()) {
    				    		Entry<String, JsonNode> zzField = zFields.next();
    				    		replacePlaceHolder(groupClone, groupDataNode, zzField);
    				    	}
    				    	clonedGroupsNode.add(groupClone);
    					}
    				}
    			}
    			((ObjectNode) structClonedNode).set("groups", clonedGroupsNode);
    		}
    	}
    	return objectMapper.writeValueAsString(structClonedNode);
	}
	
	/**
	 * find {...} in structure node then look for the value in data node
	 * @param node cloned node to be later returned
	 * @param dataNode data node
	 * @param field field we iterate on from the structural node
	 */
	private void replacePlaceHolder(JsonNode node,  JsonNode dataNode, Map.Entry<String, JsonNode> field) {
		Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(field.getValue().asText());
        if (matcher.find()) {
            String variableName = matcher.group(1);
            if (dataNode.has(variableName)) {
            	if(dataNode.get(variableName) instanceof TextNode) {
            		 String output = matcher.replaceFirst(dataNode.get(variableName).asText());
                     // replace value in the cloned node with the value from rawData
                     ((ObjectNode) node).put(field.getKey(), output);
            	} else if (dataNode.get(variableName) instanceof JsonNode) {
            		((ObjectNode) node).set(field.getKey(), dataNode.get(variableName));
            	}
                
            }
        }		
	}
	
}
