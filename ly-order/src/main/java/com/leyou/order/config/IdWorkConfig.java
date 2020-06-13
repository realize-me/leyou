package com.leyou.order.config;

import com.leyou.common.utils.IdWorker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdWorkProperties.class)
public class IdWorkConfig {
    /**
     * 注册IdWorker
     * @param prop
     * @return
     */
    @Bean
    public IdWorker idWorker(IdWorkProperties prop) {
        return new IdWorker(prop.getWorkerId(), prop.getDataCenterId());
    }
}
