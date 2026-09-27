package com.riksa.media.service;

import com.riksa.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Stores uploaded images on the local filesystem and reads them back.
 *
 * <p>Images are content-addressed by a generated UUID rather than by anything
 * the client sent: the original filename never reaches the disk, which removes
 * path traversal and collision handling in one step. The extension comes from
 * sniffing the bytes, so the name always describes the real content.
 *
 * <p>Local disk is deliberate for now — it needs no third-party account and
 * gives posts a real {@code http(s)} URL. It does not survive a redeploy on
 * ephemeral hosting, so production should swap this implementation for object
 * storage; nothing outside this class knows where the bytes live.
 */
@Slf4j
@Service
public class MediaStorageService {

    /** Guards {@code GET /media/{id}} against anything but a name we minted. */
    private static final Pattern STORED_NAME =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z]{3,4}$");

    private final Path root;
    private final long maxBytes;

    public MediaStorageService(
            @Value("${media.storage-dir:./uploads/media}") String storageDir,
            @Value("${media.max-bytes:2097152}") long maxBytes) {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create media storage directory " + root, e);
        }
        log.info("Media storage directory: {}", root);
    }

    public long maxBytes() {
        return maxBytes;
    }

    /**
     * @param bytes the uploaded file's full content
     * @return the stored name, e.g. {@code 4f3c....png}, to be used as the id
     * @throws IllegalArgumentException when the content is empty, too large, or
     *                                  not one of the accepted image formats
     */
    public String store(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "Image must be at most " + (maxBytes / 1024 / 1024) + "MB");
        }

        ImageType type = ImageType.detect(bytes);
        if (type == null) {
            throw new IllegalArgumentException("File must be a JPEG, PNG, GIF or WebP image");
        }

        String name = UUID.randomUUID() + "." + type.extension();
        Path destination = root.resolve(name);
        try {
            Path temp = Files.createTempFile(root, "upload-", ".part");
            Files.write(temp, bytes);
            Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image " + name, e);
        }
        return name;
    }

    /**
     * @param id a name previously returned by {@link #store(byte[])}
     * @throws ResourceNotFoundException when the id is malformed or absent
     */
    public StoredImage load(String id) {
        if (id == null || !STORED_NAME.matcher(id).matches()) {
            throw new ResourceNotFoundException("Image not found: " + id);
        }

        Path file = root.resolve(id).normalize();
        // resolve() on a name this pattern accepts cannot escape, but the check
        // keeps that guarantee local rather than depending on the regex.
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Image not found: " + id);
        }

        String extension = id.substring(id.lastIndexOf('.') + 1);
        ImageType type = ImageType.fromExtension(extension);
        if (type == null) {
            throw new ResourceNotFoundException("Image not found: " + id);
        }

        try {
            return new StoredImage(Files.readAllBytes(file), type.contentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read image " + id, e);
        }
    }

    /** Returns true only for a generated name that currently exists in storage. */
    public boolean exists(String id) {
        if (id == null || !STORED_NAME.matcher(id).matches()) {
            return false;
        }

        Path file = root.resolve(id).normalize();
        String extension = id.substring(id.lastIndexOf('.') + 1);
        return file.startsWith(root)
                && ImageType.fromExtension(extension) != null
                && Files.isRegularFile(file);
    }

    /** Reads image dimensions from metadata without decoding the full bitmap. */
    public ImageDimensions dimensions(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width > 0 && height > 0 ? new ImageDimensions(width, height) : null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ignored) {
            // Some accepted formats (notably WebP without an ImageIO plugin) do
            // not expose dimensions here. Upload still succeeds, with null dims.
            return null;
        }
    }

    public record StoredImage(byte[] bytes, String contentType) {}

    public record ImageDimensions(int width, int height) {}
}
