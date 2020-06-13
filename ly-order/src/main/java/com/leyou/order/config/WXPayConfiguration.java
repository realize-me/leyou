package com.leyou.order.config;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WXPayConfiguration {
    /**
     * WXPay的配置类注入到Spring容器中
     * @return
     */
    @Bean
    @ConfigurationProperties(prefix = "ly.pay")
    public PayConfig payConfig() {
        return new PayConfig();
    }

    /**
     * 将WXPay注入到spring容器中
     * @param payConfig
     * @return
     */
    @Bean
    public WXPay wxPay(PayConfig payConfig) {
        return new WXPay(payConfig, WXPayConstants.SignType.HMACSHA256);
    }

}
