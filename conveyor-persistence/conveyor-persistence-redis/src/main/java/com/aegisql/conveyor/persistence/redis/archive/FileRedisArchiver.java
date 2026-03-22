package com.aegisql.conveyor.persistence.redis.archive;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.utils.CartOutputStream;
import com.aegisql.conveyor.persistence.utils.PersistUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileRedisArchiver<K> extends AbstractRedisArchiver<K> {

    private final BinaryLogConfiguration binaryLogConfiguration;

    public FileRedisArchiver(RedisArchiveAccess<K> archiveAccess, BinaryLogConfiguration binaryLogConfiguration) {
        super(archiveAccess);
        this.binaryLogConfiguration = binaryLogConfiguration;
    }

    @Override
    public void archiveParts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try {
            Collection<Collection<Long>> balanced = PersistUtils.balanceIdList(new LinkedHashSet<>(ids), binaryLogConfiguration.getBucketSize());
            ArrayList<Cart<K, ?, Object>> carts = new ArrayList<>();
            for (Collection<Long> bucket : balanced) {
                carts.addAll(persistence.getParts(bucket));
            }
            archiveCarts(carts);
            archiveAccess.deleteParts(ids);
        } catch (IOException e) {
            throw new PersistenceException("Error saving carts to Redis archive file", e);
        }
    }

    @Override
    public void archiveKeys(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (K key : keys) {
            if (key != null) {
                ids.addAll(persistence.getAllPartIds(key));
            }
        }
        archiveParts(ids);
    }

    @Override
    public void archiveCompleteKeys(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        archiveAccess.deleteCompletedKeys(keys);
    }

    @Override
    public void archiveExpiredParts() {
        Collection<Long> ids = archiveAccess.expiredPartIds();
        archiveParts(ids);
    }

    @Override
    public void archiveAll() {
        try {
            ArrayList<Cart<K, ?, Object>> carts = new ArrayList<>(persistence.getAllParts());
            carts.addAll(persistence.getAllStaticParts());
            archiveCarts(carts);
            archiveAccess.deleteAll();
        } catch (IOException e) {
            throw new PersistenceException("Error saving carts to Redis archive file", e);
        }
    }

    private void archiveCarts(Collection<Cart<K, ?, Object>> carts) throws IOException {
        if (carts.isEmpty()) {
            return;
        }
        ensureParentDirectories();
        long currentSize = currentFileSize();
        CartOutputStream<K, Object> output = openCartOutputStream();
        try {
            for (Cart<K, ?, Object> cart : carts) {
                currentSize += output.writeCart(cart);
                if (currentSize > binaryLogConfiguration.getMaxSize()) {
                    output.close();
                    rollCurrentFile();
                    output = openCartOutputStream();
                    currentSize = currentFileSize();
                }
            }
        } finally {
            output.close();
        }
    }

    private void ensureParentDirectories() throws IOException {
        Path primary = Path.of(binaryLogConfiguration.getFilePath()).getParent();
        if (primary != null) {
            Files.createDirectories(primary);
        }
        Path moveTo = Path.of(binaryLogConfiguration.getMoveToPath());
        Files.createDirectories(moveTo);
    }

    private long currentFileSize() throws IOException {
        Path file = Path.of(binaryLogConfiguration.getFilePath());
        return Files.exists(file) ? Files.size(file) : 0L;
    }

    private CartOutputStream<K, Object> openCartOutputStream() throws IOException {
        FileOutputStream fos = new FileOutputStream(binaryLogConfiguration.getFilePath(), true);
        return new CartOutputStream<>(castCartConverter(binaryLogConfiguration.getCartConverter()), fos);
    }

    private void rollCurrentFile() throws IOException {
        Path source = Path.of(binaryLogConfiguration.getFilePath());
        if (!Files.exists(source) || Files.size(source) == 0L) {
            return;
        }
        Path stamped = Path.of(binaryLogConfiguration.getStampedFilePath());
        Path stampedParent = stamped.getParent();
        if (stampedParent != null) {
            Files.createDirectories(stampedParent);
        }
        Files.copy(source, stamped, StandardCopyOption.REPLACE_EXISTING);
        Files.newOutputStream(source, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING).close();
        if (binaryLogConfiguration.isZipFile()) {
            zipStampedFile(stamped);
        }
    }

    private void zipStampedFile(Path stamped) throws IOException {
        Path zipFile = Path.of(stamped.toString() + ".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile.toFile()));
             FileInputStream fileInputStream = new FileInputStream(stamped.toFile())) {
            zipOutputStream.putNextEntry(new ZipEntry(stamped.getFileName().toString()));
            fileInputStream.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        }
        Files.deleteIfExists(stamped);
    }

    @SuppressWarnings("unchecked")
    private CartToBytesConverter<K, ?, Object> castCartConverter(CartToBytesConverter<?, ?, ?> converter) {
        return (CartToBytesConverter<K, ?, Object>) converter;
    }
}
