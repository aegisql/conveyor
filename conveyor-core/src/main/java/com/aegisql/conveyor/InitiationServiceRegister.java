package com.aegisql.conveyor;

import java.util.*;

enum InitiationServiceRegister {
    SERVICES;

    private final ServiceLoader<ConveyorInitiatingService> serviceLoader = ServiceLoader.load(ConveyorInitiatingService.class);

    public Set<String> getLoadedConveyorNames() {
        var set = new HashSet<String>();
        serviceLoader.iterator().forEachRemaining(service->set.add(service.getConveyor().getName()));
        return set;
    }

    public List<ConveyorInitiatingService> getLoadedConveyorServices() {
        var set = new ArrayList<ConveyorInitiatingService>();
        serviceLoader.iterator().forEachRemaining(service->{
            set.add(service);
        });
        return set;
    }

    public void reload() {
        serviceLoader.reload();
    }

}
