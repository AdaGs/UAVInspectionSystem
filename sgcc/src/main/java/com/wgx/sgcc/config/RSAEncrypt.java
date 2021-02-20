package com.gjdw.stserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rsaencrypt")
public class RSAEncrypt {
    private String private_password;

    public String getPrivate_password() {
        return private_password;
    }

    public void setPrivate_password(String private_password) {
        this.private_password = private_password;
    }
}
