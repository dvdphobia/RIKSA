package com.riksa.social.service;

import com.riksa.social.entity.Category;
import com.riksa.social.entity.UserCategoryMembership;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserCategoryMembershipRepository membershipRepository;

    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Category joinCategory(UUID userId, UUID categoryId) {
        if (membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            throw new IllegalStateException("Already a member of this category");
        }
        UserCategoryMembership membership = new UserCategoryMembership();
        membership.setUserId(userId);
        membership.setCategoryId(categoryId);
        membershipRepository.save(membership);

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    @Transactional
    public Category leaveCategory(UUID userId, UUID categoryId) {
        if (!membershipRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            throw new IllegalStateException("Not a member of this category");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        // A founder leaving would strand the community with nobody to answer for it.
        if (userId.equals(category.getCreatedBy())) {
            throw new IllegalStateException("You can't leave a community you created");
        }
        membershipRepository.deleteByUserIdAndCategoryId(userId, categoryId);
        return category;
    }

    public boolean isMember(UUID userId, UUID categoryId) {
        if (userId == null) return false;
        return membershipRepository.existsByUserIdAndCategoryId(userId, categoryId);
    }

    public int getMemberCount(UUID categoryId) {
        return membershipRepository.countByCategoryId(categoryId);
    }
}