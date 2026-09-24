package com.riksa.social.repository;

import com.riksa.social.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByOrderByNameAsc();
    Optional<Category> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    long countByOfficialTrue();
    long countByCreatedBy(UUID createdBy);
}