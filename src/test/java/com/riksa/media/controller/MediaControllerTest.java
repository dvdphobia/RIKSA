package com.riksa.media.controller;

import com.riksa.media.dto.MediaUploadResponse;
import com.riksa.media.entity.MediaAsset;
import com.riksa.media.repository.MediaAssetRepository;
import com.riksa.media.service.MediaPublicUrlFactory;
import com.riksa.media.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MediaControllerTest {

    @TempDir
    Path dir;

    @Test
    void uploadReturnsPngDimensions() throws IOException {
        MediaUploadResponse response = upload("png");

        assertThat(response.url()).isEqualTo("https://api.example.test/api/v1/media/" + response.id());
        assertThat(response.id()).endsWith(".png");
        assertThat(response.width()).isEqualTo(7);
        assertThat(response.height()).isEqualTo(3);
    }

    @Test
    void uploadReturnsJpegDimensions() throws IOException {
        MediaUploadResponse response = upload("jpg");

        assertThat(response.id()).endsWith(".jpg");
        assertThat(response.width()).isEqualTo(7);
        assertThat(response.height()).isEqualTo(3);
    }

    private MediaUploadResponse upload(String format) throws IOException {
        BufferedImage image = new BufferedImage(7, 3, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        MediaStorageService storage = new MediaStorageService(dir.toString(), 1024 * 1024);
        MediaAssetRepository assetRepository = mock(MediaAssetRepository.class);
        MediaController controller = new MediaController(
                storage,
                new MediaPublicUrlFactory("https://api.example.test"),
                assetRepository);
        UUID ownerId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "fixture." + format,
                "image/" + (format.equals("jpg") ? "jpeg" : "png"), output.toByteArray());
        MediaUploadResponse response = controller.upload(ownerId, file);
        ArgumentCaptor<MediaAsset> assetCaptor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(assetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getOwnerId()).isEqualTo(ownerId);
        assertThat(assetCaptor.getValue().getId()).isEqualTo(response.id());
        assertThat(assetCaptor.getValue().getPublicUrl()).isEqualTo(response.url());
        return response;
    }
}
