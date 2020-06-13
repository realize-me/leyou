package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import com.netflix.discovery.converters.Auto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("notify")
public class NotifyController {
    @Autowired
    private OrderService orderService;
    @GetMapping("{id}")
    public String hello(@PathVariable("id") Long id) {
        return "id: " + id;
    }

    /**
     * 微信支付的成功回调
     * @param result
     * @return
     */
    @GetMapping(value = "pay",produces = "application/xml")
    public Map<String, String> payNotify(@RequestBody Map result) {
        orderService.handleNotify(result);
        log.info("[支付回调] 微信支付回调，结果:{}", result);
        // 返回成功
        Map<String, String> map = new HashMap<>();
        map.put("return_code", "SUCCESS");
        map.put("return_msg", "OK");
        return map;
    }
}
