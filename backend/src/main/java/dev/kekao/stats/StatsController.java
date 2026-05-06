package dev.kekao.stats;

import dev.kekao.stats.StatsDtos.DashboardView;
import dev.kekao.stats.StatsDtos.OverviewView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    @Autowired
    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard(@AuthenticationPrincipal Jwt jwt) {
        long userId = Long.parseLong(jwt.getSubject());
        return service.dashboard(userId);
    }

    @GetMapping("/overview")
    public OverviewView overview(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(value = "days", defaultValue = "30") int days) {
        long userId = Long.parseLong(jwt.getSubject());
        return service.overview(userId, days);
    }
}
