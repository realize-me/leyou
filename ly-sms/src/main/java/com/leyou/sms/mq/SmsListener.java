package com.leyou.sms.mq;

import com.aliyuncs.exceptions.ClientException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.utils.SmsUtils;
import com.rabbitmq.tools.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@EnableConfigurationProperties(SmsProperties.class)
@Component
public class SmsListener {
    @Autowired
    private SmsUtils smsUtils;
    @Autowired
    private SmsProperties prop;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "sms.verify.code.queue",durable = "true"),
                    exchange = @Exchange(name = "ly.sms.exchange",type = ExchangeTypes.TOPIC),
                    key = {"sms.verify.code"}
            )
    )
    public void listenItemInsertOrUpdate(Map<String,String> msg){
        if(msg==null){
            return;
        }
        String phone = msg.remove("phone");
        if(StringUtils.isBlank(phone)){
            return;
        }

        smsUtils.sendSms(phone, prop.getSignName(), prop.getVerifyCodeTemplate(), JsonUtils.serialize(msg));
    }
}
