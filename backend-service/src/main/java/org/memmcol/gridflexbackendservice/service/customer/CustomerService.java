package org.memmcol.gridflexbackendservice.service.customer;

import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public interface CustomerService {


    Map<String, Object> createCustomer(Customer request);

    Map<String, Object> updateCustomer(Customer request);

    Map<String, Object> allCustomers(int page, int size, String firstname, String lastname, String accountNumber, String assignedStatus, String customerId);

    Map<String, Object> singleCustomer(UUID id);

    Map<String, Object> changeState(UUID customerId, String status, String reason) throws MissingServletRequestParameterException;

    Map<String, Object> bulkUpload(MultipartFile file) throws IOException;
}
