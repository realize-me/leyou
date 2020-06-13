package com.leyou.test;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class SmsTest {
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    void test(){

        Map<String, String> msg = new HashMap<>();

        msg.put("phone", "18713986912");
        msg.put("code", "222666");
        amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);
    }
}
