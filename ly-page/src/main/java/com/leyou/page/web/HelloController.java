package com.leyou.page.web;

import com.leyou.page.pojo.User;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;


@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model){
        User user = new User("jack", 21, null);
        User user2 = new User("李小龙", 30, null);

        model.addAttribute("users", Arrays.asList(user, user2));
        model.addAttribute("msg", "hello thymeleaf");

        return "hello";
    }
}