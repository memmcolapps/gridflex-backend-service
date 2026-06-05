package org.memmcol.gridflexbackendservice.util;

import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;

public class HandlePermission {

    public static void perm(String nodeType) {
          if(nodeType == null || (!nodeType.equalsIgnoreCase("Root")
                    && !nodeType.equalsIgnoreCase("Business hub")
                    && !nodeType.equalsIgnoreCase("Service center")
                    && !nodeType.equalsIgnoreCase("Region"))){
        throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }
    }
}
