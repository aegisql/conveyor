package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

public final class CredentialStores {

    private CredentialStores() {
    }

    public static CredentialStore defaultStore(Path databasePath) {
        return defaultStore(
                databasePath,
                System.getProperty("os.name", ""),
                new SwingMasterPasswordProvider(),
                MacOsKeychainCredentialStore.isSupported(),
                WindowsDpapiCredentialStore.isSupported(),
                LinuxSecretToolCredentialStore.isSupported()
        );
    }

    static CredentialStore defaultStore(Path databasePath,
                                        String osName,
                                        MasterPasswordProvider masterPasswordProvider,
                                        boolean macSupported,
                                        boolean windowsSupported,
                                        boolean linuxSupported) {
        return defaultStore(
                databasePath,
                osName,
                masterPasswordProvider,
                macSupported,
                windowsSupported,
                linuxSupported,
                MacOsKeychainCredentialStore::new,
                () -> new WindowsDpapiCredentialStore(databasePath),
                LinuxSecretToolCredentialStore::new
        );
    }

    static CredentialStore defaultStore(Path databasePath,
                                        String osName,
                                        MasterPasswordProvider masterPasswordProvider,
                                        boolean macSupported,
                                        boolean windowsSupported,
                                        boolean linuxSupported,
                                        Supplier<CredentialStore> macSupplier,
                                        Supplier<CredentialStore> windowsSupplier,
                                        Supplier<CredentialStore> linuxSupplier) {
        if (isMacOs(osName) && macSupported) {
            return macSupplier.get();
        }
        if (isWindows(osName) && windowsSupported) {
            return windowsSupplier.get();
        }
        if (isLinux(osName) && linuxSupported) {
            return linuxSupplier.get();
        }
        return new MasterPasswordCredentialStore(databasePath, masterPasswordProvider);
    }

    private static boolean isMacOs(String osName) {
        return normalized(osName).contains("mac");
    }

    private static boolean isWindows(String osName) {
        return normalized(osName).contains("win");
    }

    private static boolean isLinux(String osName) {
        return normalized(osName).contains("linux");
    }

    private static String normalized(String osName) {
        return osName == null ? "" : osName.toLowerCase(Locale.ROOT);
    }
}
