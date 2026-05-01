package dev.kekao.admin;

import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Promotes the user identified by {@code KEKAO_ADMIN_EMAIL} (env or
 * {@code kekao.admin.bootstrap-email} property) to {@link UserRole#ROLE_ADMIN}.
 *
 * <p>Idempotent: if the user is already an admin, or no email is configured,
 * the listener is a no-op. The user must already exist (register through the
 * normal flow first); we never auto-create accounts here.
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final AdminProperties props;

    public AdminBootstrap(UserRepository users, AdminProperties props) {
        this.users = users;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void promoteConfiguredAdmin() {
        String email = props.bootstrapEmail();
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        Optional<UserEntity> found = users.findByEmail(normalized);
        if (found.isEmpty()) {
            log.info("Admin bootstrap: user {} not found, skipping promotion", normalized);
            return;
        }
        UserEntity user = found.get();
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        user.setRole(UserRole.ROLE_ADMIN);
        users.save(user);
        log.info("Promoted user {} to ROLE_ADMIN", normalized);
    }
}
