package com.riksa.auth.dto;

import java.util.UUID;

public record ProvinceResponse(
        UUID id,
        String nameKhmer,
        String nameLatin
) {}
