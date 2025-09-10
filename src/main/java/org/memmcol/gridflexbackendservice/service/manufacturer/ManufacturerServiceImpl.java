package org.memmcol.gridflexbackendservice.service.manufacturer;

import jakarta.servlet.http.HttpServletRequest;
//import jakarta.transaction.Transactional;
import org.memmcol.gridflexbackendservice.mapper.ManufacturerMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class ManufacturerServiceImpl implements ManufacturerService {


    private static final Logger log = LoggerFactory.getLogger(ManufacturerServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ManufacturerMapper manufacturerMapper;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;


    private String manfacturerName = "Manufacturer";

    @Transactional
    @Override
    public Map<String, Object> createManufacturer(Manufacturer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            String desc = capitalizeFirstLetter(request.getName()) + "newly created";
            UserModel um = handleUserValidation();

            // check if operator exist
            Manufacturer isManufacturer = manufacturerMapper.findByName(request.getName(), um.getOrgId());
            if (isManufacturer != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(manfacturerName + " " + status.getExistDesc());
            }

            request.setOrgId(um.getOrgId());

            // Insert into operators
            manufacturerMapper.insertManufacturer(request);
            UUID id = request.getId();

            Manufacturer manufacturer = manufacturerMapper.findById(id, um.getOrgId());
//            handleAddCache(user);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(manfacturerName);
            auditNotificationDTO.setManufacturer(manufacturer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), manfacturerName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating manufacturer [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateManufacturer(Manufacturer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            String desc = capitalizeFirstLetter(request.getName()) + "edited";
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            // check if operator exist
            Manufacturer isManufacturer = manufacturerMapper.findById(request.getId(), um.getOrgId());
            if (isManufacturer == null){
                throw new GlobalExceptionHandler.NotFoundException(manfacturerName + " " + status.getNotFoundDesc());
            }

            request.setOrgId(um.getOrgId());

            // Insert into operators
            manufacturerMapper.updateManufacturer(request);
            UUID id = request.getId();

            Manufacturer manufacturer = manufacturerMapper.findById(id, um.getOrgId());
//            handleAddCache(user);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(manfacturerName);
            auditNotificationDTO.setManufacturer(manufacturer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), manfacturerName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updating manufacturer [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getManufacturer(UUID id) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            // check if operator exist
            Manufacturer manufacturer = manufacturerMapper.findById(id, um.getOrgId());
            if (manufacturer == null){
                throw new GlobalExceptionHandler.NotFoundException(manfacturerName + " " + status.getNotFoundDesc());
            }

            return ResponseMap.response(status.getSuccessCode(), manfacturerName + " " + status.getDesc(), manufacturer);
        } catch (Exception exception) {
            log.error("Error occurred while creating manufacturer [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
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
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .isBefore(date);
                });
            }

            List<Manufacturer> filteredManufacturers = manufacturerStream.toList();

            // Pagination logic
//            int totalManufacturers = filteredManufacturers.size();
//            List<Manufacturer> paginatedManufacturers;
//            if (size == 0) {
//                paginatedManufacturers = filteredManufacturers; // Return all users
//            } else {
//                int fromIndex = Math.min(page * size, totalManufacturers);
//                int toIndex = Math.min(fromIndex + size, totalManufacturers);
//                paginatedManufacturers = filteredManufacturers.subList(fromIndex, toIndex);
//            }

            // Prepare response with pagination metadata
//            Map<String, Object> response = new HashMap<>();
//            response.put("data", paginatedManufacturers);
//            response.put("totalData", totalManufacturers);
//            response.put("page", page);
//            response.put("size", size);
//            response.put("totalPages", (int) Math.ceil((double) paginatedManufacturers.size() / size));

//            userCache.put(cacheKey, response);

            return ResponseMap.response(status.getSuccessCode(), manfacturerName + "s " + status.getDesc(), filteredManufacturers);

        } catch (Exception exception) {
            log.error("Error filtering / fetching manufacturers: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while filtering users");
            exceptionErrorLogs.setError_message(exception.getMessage());
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }
}
