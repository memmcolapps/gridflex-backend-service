package org.memmcol.gridflexbackendservice.service.hes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HesBusinessServiceImpl implements HesService{
    @Autowired
    private HesClientServiceImpl thirdPartyClient;

    @Override
    public Map<String, Object> doWork() {
        System.out.println("doWork");
        return thirdPartyClient.loadSomething();
    }

//    public String doWork() {
//        return thirdPartyClient.loadSomething();
//    }
}
