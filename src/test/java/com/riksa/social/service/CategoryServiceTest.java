package com.riksa.social.service;

import com.riksa.social.entity.Category;
import com.riksa.social.entity.UserCategoryMembership;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
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
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private UserCategoryMembershipRepository membershipRepository;

    @InjectMocks private CategoryService categoryService;

    @Test
    void joinCategory_notMember_joinsSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Physics");

        when(membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        Category result = categoryService.joinCategory(userId, categoryId);

        assertThat(result.getName()).isEqualTo("Physics");
        verify(membershipRepository).save(any(UserCategoryMembership.class));
    }

    @Test
    void joinCategory_alreadyMember_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.joinCategory(userId, categoryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already a member of this category");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void leaveCategory_member_leavesSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Math");

        when(membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)).thenReturn(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        Category result = categoryService.leaveCategory(userId, categoryId);

        assertThat(result.getName()).isEqualTo("Math");
        verify(membershipRepository).deleteByUserIdAndCategoryId(userId, categoryId);
    }

    @Test
    void leaveCategory_notMember_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.leaveCategory(userId, categoryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not a member of this category");
    }

    @Test
    void isMember_returnsCorrectResult() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)).thenReturn(true);

        assertThat(categoryService.isMember(userId, categoryId)).isTrue();
    }

    @Test
    void getMemberCount_returnsCorrectCount() {
        UUID categoryId = UUID.randomUUID();
        when(membershipRepository.countByCategoryId(categoryId)).thenReturn(42);

        assertThat(categoryService.getMemberCount(categoryId)).isEqualTo(42);
    }
}