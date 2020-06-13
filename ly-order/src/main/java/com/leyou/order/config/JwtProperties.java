package com.leyou.order.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {
    private String pubKeyPath;
    private String cookieName;
    private PublicKey publicKey;

    // 对象实例化后读取公钥和私钥
    @PostConstruct
    public void init() throws Exception {
        // 读取公钥私钥
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
    }
}
