package org.memmcol.gridflexbackendservice.util;

import java.util.HashMap;
import java.util.Map;




public class ResponseMap {
	
	public static Map<String, Object> response(String responseCode, String responseDesc, Object responseData) {
	    Map<String, Object> responseMap = new HashMap<>();
	    responseMap.put("responsecode", responseCode);
	    responseMap.put("responsedesc", responseDesc);
	    responseMap.put("responsedata", responseData);
	    return responseMap;
	}
	
}
