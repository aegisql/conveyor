package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;

import java.util.List;

public interface PersistenceBackend {

    ConnectionStatus connectionStatus(PersistenceProfile profile);

    PersistenceSnapshot inspect(PersistenceProfile profile, int rowLimit, int pageIndex);

    List<String> lookupDatabases(PersistenceProfile profile);

    List<String> lookupSchemas(PersistenceProfile profile);

    List<String> lookupPersistenceNames(PersistenceProfile profile);

    String initializationScript(PersistenceProfile profile);

    String initializationJavaCode(PersistenceProfile profile);

    default InitializationPreview initializationPreview(PersistenceProfile profile) {
        return new InitializationPreview(initializationScript(profile), initializationJavaCode(profile));
    }

    void initialize(PersistenceProfile profile);

    void archiveExpired(PersistenceProfile profile);

    void archiveAll(PersistenceProfile profile);
}
