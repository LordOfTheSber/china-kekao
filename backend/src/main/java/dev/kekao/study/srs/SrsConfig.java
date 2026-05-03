package dev.kekao.study.srs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SrsConfig {

    @Bean
    public SrsAlgorithm srsAlgorithm() {
        return new FsrsAlgorithm();
    }
}
