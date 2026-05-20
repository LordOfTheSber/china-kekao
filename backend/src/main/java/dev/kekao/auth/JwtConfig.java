package dev.kekao.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    RSAKey rsaKey(AuthProperties props) throws Exception {
        if (hasText(props.privateKey()) && hasText(props.publicKey())) {
            RSAPrivateKey priv = parsePrivate(props.privateKey());
            RSAPublicKey pub = parsePublic(props.publicKey());
            return new RSAKey.Builder(pub).privateKey(priv).keyID("kekao-auth-key").build();
        }
        log.warn("JWT_PRIVATE_KEY/JWT_PUBLIC_KEY not configured; generating ephemeral RSA keypair. "
                + "Tokens will be invalidated on every restart. Set the env vars in production.");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> source) {
        return new NimbusJwtEncoder(source);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey) throws Exception {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static RSAPrivateKey parsePrivate(String pem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static RSAPublicKey parsePublic(String pem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String stripPem(String pem) {
        return pem
                .replace("\\r\\n", "")
                .replace("\\n", "")
                .replace("\\r", "")
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");
    }
}
