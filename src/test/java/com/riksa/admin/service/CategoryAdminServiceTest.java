package com.riksa.admin.service;

import com.riksa.admin.dto.CategoryRequest;
import com.riksa.admin.dto.CategoryResponse;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.service.UploadedMediaUrlValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryAdminServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private UploadedMediaUrlValidator uploadedMediaUrlValidator;

    @InjectMocks private CategoryAdminService categoryAdminService;

    @Test
    void create_validRequest_createsCategory() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Physics");
        req.setSlug("physics");
        req.setDescription("Physics discussions");
        req.setIconUrl("physics.png");

        when(categoryRepository.existsByName("Physics")).thenReturn(false);
        when(categoryRepository.existsBySlug("physics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse result = categoryAdminService.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Physics");
        assertThat(result.getSlug()).isEqualTo("physics");
    }

    @Test
    void create_duplicateName_throwsException() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Physics");
        req.setSlug("physics-new");

        when(categoryRepository.existsByName("Physics")).thenReturn(true);

        assertThatThrownBy(() -> categoryAdminService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name already exists: Physics");
    }

    @Test
    void create_duplicateSlug_throwsException() {
        CategoryRequest req = new CategoryRequest();
        req.setName("Physics2");
        req.setSlug("physics");

        when(categoryRepository.existsByName("Physics2")).thenReturn(false);
        when(categoryRepository.existsBySlug("physics")).thenReturn(true);

        assertThatThrownBy(() -> categoryAdminService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category slug already exists: physics");
    }

    @Test
    void update_existingCategory_updatesFields() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Old Name");
        existing.setSlug("old-slug");

        CategoryRequest req = new CategoryRequest();
        req.setName("New Name");
        req.setSlug("new-slug");
        req.setDescription("Updated description");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("New Name")).thenReturn(false);
        when(categoryRepository.existsBySlug("new-slug")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = categoryAdminService.update(id, req);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getSlug()).isEqualTo("new-slug");
    }

    @Test
    void update_nonExistentCategory_throwsException() {
        UUID id = UUID.randomUUID();
        CategoryRequest req = new CategoryRequest();
        req.setName("Test");
        req.setSlug("test");

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryAdminService.update(id, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void update_duplicateNameFromAnotherCategory_throwsException() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Old Name");
        existing.setSlug("old-slug");

        CategoryRequest req = new CategoryRequest();
        req.setName("Taken Name");
        req.setSlug("old-slug"); // same slug, no conflict

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Taken Name")).thenReturn(true);

        assertThatThrownBy(() -> categoryAdminService.update(id, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category name already exists");
    }

    @Test
    void delete_existingCategory_deletesIt() {
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        categoryAdminService.delete(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_nonExistentCategory_throwsException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryAdminService.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void create_storesRawBackgroundAndRejectsUnknownKey() {
        String image = "https://api.example.test/api/v1/media/image.png";
        when(uploadedMediaUrlValidator.normalize(image)).thenReturn(image);
        when(categoryRepository.existsByName("Biology")).thenReturn(false);
        when(categoryRepository.existsBySlug("biology")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest req = new CategoryRequest();
        req.setName("Biology");
        req.setSlug("biology");
        req.setBackgroundKey("biology");
        req.setBackgroundImageUrl(image);

        CategoryResponse created = categoryAdminService.create(req);

        assertThat(created.getBackgroundKey()).isEqualTo("biology");
        assertThat(created.getBackgroundImageUrl()).isEqualTo(image);

        req.setBackgroundKey("latin");
        assertThatThrownBy(() -> categoryAdminService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backgroundKey must be one of");
    }
}