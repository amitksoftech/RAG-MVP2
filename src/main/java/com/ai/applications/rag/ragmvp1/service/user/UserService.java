package com.ai.applications.rag.ragmvp1.service.user;

import com.ai.applications.rag.ragmvp1.config.BootstrapProperties;
import com.ai.applications.rag.ragmvp1.domain.entity.AppUser;
import com.ai.applications.rag.ragmvp1.domain.entity.RoleName;
import com.ai.applications.rag.ragmvp1.repository.AppUserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties bootstrapProperties;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, BootstrapProperties bootstrapProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    @PostConstruct
    @Transactional
    public void bootstrapUsers() {
        if (!userRepository.existsByUsername(bootstrapProperties.adminUsername())) {
            userRepository.save(new AppUser(
                    bootstrapProperties.adminUsername(),
                    passwordEncoder.encode(bootstrapProperties.adminPassword()),
                    RoleName.ROLE_ADMIN));
        }

        if (!userRepository.existsByUsername(bootstrapProperties.userUsername())) {
            userRepository.save(new AppUser(
                    bootstrapProperties.userUsername(),
                    passwordEncoder.encode(bootstrapProperties.userPassword()),
                    RoleName.ROLE_USER));
        }
    }
}
