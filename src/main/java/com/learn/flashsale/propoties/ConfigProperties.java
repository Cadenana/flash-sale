package com.learn.flashsale.propoties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "aihei")
@Component
@Data
public class ConfigProperties {
    private String[] nonFilterPath;

    private String[] jwtFilterPath;

    private String[] staticSource;

    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}