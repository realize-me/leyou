package com.leyou.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;

@EnableZuulProxy
@SpringCloudApplication
public class LeyouGateway {
    public static void main(String[] args) {
        SpringApplication.run(LeyouGateway.class);
    }
}
