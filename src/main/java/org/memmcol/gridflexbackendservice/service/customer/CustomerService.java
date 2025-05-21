package org.memmcol.gridflexbackendservice.service.customer;

import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public interface CustomerService {


    Map<String, Object> createCustomer(Customer request);

    Map<String, Object> updateCustomer(Customer request);

    Map<String, Object> allCustomers(int page, int size, String firstname, String lastname, String meterNumber, String accountNumber, Boolean meterAssigned);

    Map<String, Object> singleCustomer(UUID customerId);

    Map<String, Object> changeState(UUID customerId, Boolean status, String reason);

    Map<String, Object> bulkUpload(MultipartFile file) throws IOException;
}
