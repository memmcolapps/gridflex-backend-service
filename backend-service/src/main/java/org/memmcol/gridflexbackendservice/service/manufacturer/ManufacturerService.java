package org.memmcol.gridflexbackendservice.service.manufacturer;

import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;

import java.util.Map;
import java.util.UUID;

public interface ManufacturerService {
    Map<String, Object> createManufacturer(Manufacturer request);

    Map<String, Object> updateManufacturer(Manufacturer request);

//    Map<String, Object> manageManufacturerState(UUID id, Boolean status);

    Map<String, Object> getManufacturer(UUID id);

    Map<String, Object> getManufacturers(String name, String manufacturerId, String contactPerson, String dateAdded);
}
