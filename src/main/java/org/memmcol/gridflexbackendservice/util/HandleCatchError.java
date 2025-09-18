//package org.memmcol.gridflexbackendservice.util;
//
//import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
//import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//@Component
//public class HandleCatchError {
//
//    private static ExceptionAuditRepository exceptionAuditRepository;
//
//    public static void catchError(Exception exception) {
//        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
//        errorLog.setDescription("Error occurred while logout");
//        errorLog.setError_message(exception.getMessage());
//        errorLog.setError(exception.toString());
//        exceptionAuditRepository.save(errorLog);
//    }
//
//    public static void setExceptionAuditRepository(ExceptionAuditRepository exceptionAuditRepository) {
//        HandleCatchError.exceptionAuditRepository = exceptionAuditRepository;
//    }
//}
