package com.riksa.auth.security;

import com.riksa.auth.repository.UserRepository;
import com.riksa.common.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSecurityStateService {

    private final UserRepository userRepository;

    @Cacheable(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()",
            condition = "@redisCacheFeatureSwitch.enabled()")
    public UserSecurityState get(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserSecurityState(!user.isSuspended(), user.getRole().name()))
                .orElseGet(() -> new UserSecurityState(false, "USER"));
    }
}
