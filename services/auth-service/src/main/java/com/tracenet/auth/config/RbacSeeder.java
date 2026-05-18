package com.tracenet.auth.config;

import com.tracenet.auth.entity.Permission;
import com.tracenet.auth.entity.Role;
import com.tracenet.auth.repository.PermissionRepository;
import com.tracenet.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class RbacSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public RbacSeeder(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        seedPermissions();
        seedRoles();
    }

    private void seedPermissions() {
        List<String> permissions = List.of(
                "VIEW_TRACES",
                "VIEW_ERRORS",
                "VIEW_SLOW_TRACES",
                "INGEST_TRACES",
                "MANAGE_USERS",
                "MANAGE_ALERTS",
                "VIEW_AUDIT_LOGS"
        );

        for (String permissionName : permissions) {
            if (!permissionRepository.existsByName(permissionName)) {
                permissionRepository.save(new Permission(permissionName));
            }
        }
    }

    private void seedRoles() {
        createRoleIfMissing(
                "ADMIN",
                Set.of(
                        "VIEW_TRACES",
                        "VIEW_ERRORS",
                        "VIEW_SLOW_TRACES",
                        "INGEST_TRACES",
                        "MANAGE_USERS",
                        "MANAGE_ALERTS",
                        "VIEW_AUDIT_LOGS"
                )
        );

        createRoleIfMissing(
                "SRE",
                Set.of(
                        "VIEW_TRACES",
                        "VIEW_ERRORS",
                        "VIEW_SLOW_TRACES",
                        "INGEST_TRACES",
                        "MANAGE_ALERTS"
                )
        );

        createRoleIfMissing(
                "DEVELOPER",
                Set.of(
                        "VIEW_TRACES",
                        "VIEW_ERRORS",
                        "VIEW_SLOW_TRACES"
                )
        );

        createRoleIfMissing(
                "VIEWER",
                Set.of("VIEW_TRACES")
        );
    }

    private void createRoleIfMissing(String roleName, Set<String> permissionNames) {
        if (roleRepository.existsByName(roleName)) {
            return;
        }

        Role role = new Role(roleName);
        Set<Permission> permissions = new LinkedHashSet<>();

        for (String permissionName : permissionNames) {
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseThrow(() -> new IllegalStateException("Missing permission: " + permissionName));

            permissions.add(permission);
        }

        role.setPermissions(permissions);
        roleRepository.save(role);
    }
}