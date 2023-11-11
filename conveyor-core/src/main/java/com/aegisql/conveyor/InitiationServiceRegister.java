package com.aegisql.conveyor;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

enum InitiationServiceRegister {
    SERVICES;

    private final ServiceLoader<ConveyorInitiatingService> serviceLoader = ServiceLoader.load(ConveyorInitiatingService.class);

    public Set<String> getLoadedConveyorNames() {
        var set = new HashSet<String>();
        serviceLoader.iterator().forEachRemaining(service->set.add(service.getName()));
        return set;
    }

    public void reload() {
        serviceLoader.reload();
    }

}
