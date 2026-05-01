package dev.kekao.admin.imports;

import dev.kekao.admin.AdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ImportProperties.class, AdminProperties.class})
public class ImportConfig {
}
