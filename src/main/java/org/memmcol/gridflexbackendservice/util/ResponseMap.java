package org.memmcol.gridflexbackendservice.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResponseMap {
	
	public static Map<String, Object> response(String responseCode, String responseDesc, Object responseData) {
	    Map<String, Object> responseMap = new HashMap<>();
	    responseMap.put("responsecode", responseCode);
	    responseMap.put("responsedesc", responseDesc);
	    responseMap.put("responsedata", responseData);
	    return responseMap;
	}

//	public static Map<String, Object> bulkResponse(UUID id, String responseDesc, String responseData) {
//		Map<String, Object> responseBulkMap = new HashMap<>();
//		responseBulkMap.put("id", id);
//		responseBulkMap.put("responsedesc", responseDesc);
//		responseBulkMap.put("responsedata", responseData);
//		return responseBulkMap;
//	}
	
}
