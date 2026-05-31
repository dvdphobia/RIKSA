package com.riksa.auth.repository;

import com.riksa.auth.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;


@Repository

public interface ProvinceRepository extends JpaRepository<Province, UUID> {
    // boolean existsByNameKhmer(String nameKhmer);
    // boolean existsByNameLatin(String nameLatin);


}
