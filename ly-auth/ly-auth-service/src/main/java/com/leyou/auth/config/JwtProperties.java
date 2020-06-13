package com.leyou.auth.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {
    private String secret;
    private String pubKeyPath;
    private String priKeyPath;
    private int expire;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    // 对象实例化后读取公钥和私钥
    @PostConstruct
    public void init() throws Exception {
        // 公钥私钥不存在
        File pubKey = new File(pubKeyPath);
        File priKey = new File(priKeyPath);
        if (!pubKey.exists() || !priKey.exists()) {
            RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
        }

        // 读取公钥私钥
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);

    }
}
