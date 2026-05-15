package dev.kekao.achievement;

import dev.kekao.achievement.AchievementDtos.AchievementListResponse;
import dev.kekao.achievement.AchievementDtos.AchievementView;
import dev.kekao.achievement.AchievementDtos.ClaimRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService service;

    @Autowired
    public AchievementController(AchievementService service) {
        this.service = service;
    }

    @GetMapping
    public AchievementListResponse list(@AuthenticationPrincipal Jwt jwt) {
        return service.listForUser(userId(jwt));
    }

    @PostMapping("/claim/{code}")
    public ResponseEntity<Map<String, Object>> claim(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable("code") String code,
                                                     @RequestBody(required = false) ClaimRequest req) {
        Optional<AchievementView> unlocked = service.claim(userId(jwt), code, req);
        if (unlocked.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "unlocked", true,
                    "achievement", unlocked.get()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "unlocked", false,
                "code", code
        ));
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
