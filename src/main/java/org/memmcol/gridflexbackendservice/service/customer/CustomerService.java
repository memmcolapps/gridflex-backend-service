package org.memmcol.gridflexbackendservice.service.customer;

import org.memmcol.gridflexbackendservice.model.customer.Customer;

import java.util.Map;

public interface CustomerService {


    Map<String, Object> createCustomer(Customer request);

    Map<String, Object> updateCustomer(Customer request);

    Map<String, Object> allCustomers(int page, int size, String firstname, String lastname, String meterNumber, String accountNumber, Boolean meterAssigned);

    Map<String, Object> singleCustomer(String accountNumber);

    Map<String, Object> changeState(String accountNumber, Boolean status, String reason);
}
