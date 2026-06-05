package org.memmcol.gridflexbackendservice.service.manufacturer;

import jakarta.servlet.http.HttpServletRequest;
//import jakarta.transaction.Transactional;
import org.memmcol.gridflexbackendservice.mapper.ManufacturerMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class ManufacturerServiceImpl implements ManufacturerService {


    private static final Logger log = LoggerFactory.getLogger(ManufacturerServiceImpl.class);

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private ManufacturerMapper manufacturerMapper;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private GenericHandler genericHandler;


    private String manufacturerName = "Manufacturer";

    @Transactional
    @Override
    public Map<String, Object> createManufacturer(Manufacturer request) {
        try {

            handlePayloadCheck(request);
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            String desc = capitalizeFirstLetter(request.getName()) + "newly created";
            UserModel um = handleUserValidation();
            String nodeType = um.getNodeInfo().getType();
            if(!nodeType.equalsIgnoreCase("Region")
                    && !nodeType.equalsIgnoreCase("Root")
            ){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            // check if operator exist
            Manufacturer isManufacturer = manufacturerMapper.findByName(request.getName(), um.getOrgId());
            if (isManufacturer != null){
                throw new GlobalExceptionHandler.NotFoundException(manufacturerName + " ("+request.getName()+")" + status.getExistDesc());
            }

            request.setOrgId(um.getOrgId());

            // Insert into operators
            manufacturerMapper.insertManufacturer(request);
            UUID id = request.getId();

            Manufacturer manufacturer = manufacturerMapper.findById(id, um.getOrgId());
//            handleAddCache(user);
            AuditLog auditLog = buildAuditLog(um, desc, manufacturerName, manufacturer, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), manufacturerName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating manufacturer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Creating manufacturer service failed");
            genericHandler.logAndSaveException(exception, "creating manufacturer");

            throw exception;
        }
    }

    private void handlePayloadCheck(Manufacturer request) {

        if(request.getName() == null || request.getName().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("Name field is required");
        }
        if(request.getPhoneNo() == null || request.getPhoneNo().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("Phone number field is required");
        }
        if(request.getState() == null || request.getState().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("State field is required");
        }
        if(request.getStreet() == null || request.getStreet().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("Street field is required");
        }
        if(request.getCity() == null || request.getCity().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("City field is required");
        }
        if(request.getHouseNo() == null || request.getHouseNo().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("House number field is required");
        }
        if(request.getContactPerson() == null || request.getContactPerson().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("House number field is required");
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateManufacturer(Manufacturer request) {
        try {
            handlePayloadCheck(request);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc = capitalizeFirstLetter(request.getName()) + "edited";
            UserModel um = handleUserValidation();
            String nodeType = um.getNodeInfo().getType();

            if(!nodeType.equalsIgnoreCase("Region")
                    && !nodeType.equalsIgnoreCase("Root")
            ){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            // check if operator exist
            Manufacturer isManufacturer = manufacturerMapper.getManufacturerByIdForUpdate(request.getId(), um.getOrgId());
            if (isManufacturer == null){
                throw new GlobalExceptionHandler.NotFoundException(manufacturerName + " " + status.getNotFoundDesc());
            }

            if(!isManufacturer.getName().equalsIgnoreCase(request.getName())){
                Manufacturer m = manufacturerMapper.find(request.getName());
                if (m != null){
                    throw new GlobalExceptionHandler.NotFoundException(manufacturerName + " ("+request.getName()+")" + status.getExistDesc());
                }
            }

            request.setOrgId(um.getOrgId());

            // Insert into operators
            manufacturerMapper.updateManufacturer(request);
            UUID id = request.getId();

            Manufacturer manufacturer = manufacturerMapper.findById(id, um.getOrgId());
//            handleAddCache(user);
            AuditLog auditLog = buildAuditLog(um, desc, manufacturerName, manufacturer, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), manufacturerName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updating manufacturer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Editing manufacturer service failed");
            genericHandler.logAndSaveException(exception, "editing manufacturer");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getManufacturer(UUID id) {
        try {

            UserModel um = handleUserValidation();

            // check if operator exist
            Manufacturer manufacturer = manufacturerMapper.findById(id, um.getOrgId());
            if (manufacturer == null){
                throw new GlobalExceptionHandler.NotFoundException(manufacturerName + " " + status.getNotFoundDesc());
            }

            return ResponseMap.response(status.getSuccessCode(), manufacturerName + " " + status.getDesc(), manufacturer);
        } catch (Exception exception) {
            log.error("Error occurred while creating manufacturer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching manufacturer service failed");
            genericHandler.logAndSaveException(exception, "fetching manufacturer");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getManufacturers(String name, String manufacturerId, String contactPerson, String dateAdded) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            List<Manufacturer> manufacturers = manufacturerMapper.getAllManufacturers(um.getOrgId());

            // Apply filtering
            Stream<Manufacturer> manufacturerStream = manufacturers.stream();

            if (name != null && !name.isEmpty()) {
                manufacturerStream = manufacturerStream.filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(name));
            }

            if (manufacturerId != null && !manufacturerId.isEmpty()) {
                manufacturerStream = manufacturerStream.filter(u -> u.getManufacturerId() != null && u.getManufacturerId().equalsIgnoreCase(manufacturerId));
            }

            if (contactPerson != null && !contactPerson.isEmpty()) {
                manufacturerStream = manufacturerStream.filter(u -> u.getContactPerson() != null && u.getContactPerson().equalsIgnoreCase(contactPerson));
            }

            if (dateAdded != null && !dateAdded.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate date = LocalDate.parse(dateAdded, formatter);
                manufacturerStream = manufacturerStream.filter(u -> {
                    if (u.getCreatedAt() == null) return false;
                    return !u.getCreatedAt()
//                            .toInstant()
//                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .isBefore(date);
                });
            }

            List<Manufacturer> filteredManufacturers = manufacturerStream.toList();

            return ResponseMap.response(status.getSuccessCode(), manufacturerName + "s " + status.getDesc(), filteredManufacturers);

        } catch (Exception exception) {
            log.error("Error filtering / fetching manufacturers: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching all manufacturers service failed");
            genericHandler.logAndSaveException(exception, "fetching manufacturers");
            throw exception;
        }
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Manufacturer createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setManufacturer(createdEntity);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }
}
