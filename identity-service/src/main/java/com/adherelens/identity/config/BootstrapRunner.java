package com.adherelens.identity.config;

import com.adherelens.common.model.Role;
import com.adherelens.identity.domain.Staff;
import com.adherelens.identity.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first PLATFORM_ADMIN on startup if none exists. */
@Component
public class BootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapRunner(StaffRepository staffRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${adherelens.bootstrap.admin-email}") String adminEmail,
                           @Value("${adherelens.bootstrap.admin-password}") String adminPassword) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (staffRepository.existsByRole(Role.PLATFORM_ADMIN)) {
            return;
        }
        Staff admin = new Staff();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFullName("Platform Admin");
        admin.setRole(Role.PLATFORM_ADMIN);
        staffRepository.save(admin);
        log.info("Created bootstrap platform admin: {}", adminEmail);
    }
}
