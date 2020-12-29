package org.hl7.tinkar.provider.entity.internal;

import org.hl7.tinkar.service.CachingService;
import org.hl7.tinkar.service.PrimitiveDataService;

import java.util.ServiceLoader;

public class Get implements CachingService {

    private final ServiceLoader<PrimitiveDataService> serviceLoader;
    private final PrimitiveDataService dataService;

    public Get() {
        this.serviceLoader = ServiceLoader.load(PrimitiveDataService.class);
        this.dataService = this.serviceLoader.findFirst().get();
    }

    private static Get singleton = new Get();

    @Override
    public void reset() {
        singleton = null;
    }

    public static PrimitiveDataService dataService() {
        return singleton.dataService;
    }

}
