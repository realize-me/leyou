package com.leyou.auth.web;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@EnableConfigurationProperties(JwtProperties.class)
@RestController
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProperties prop;

    @Value("${ly.jwt.cookieName}")
    private String cookieName;
    /**
     * 登陆授权
     * @param username
     * @param password
     * @return
     */
    @PostMapping("login")
    public ResponseEntity<Void> login(
            @RequestParam("username")String username,
            @RequestParam("password")String password,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        // 生成token
        String token = authService.login(username, password);
        // 将token写入cookie
        CookieUtils.setCookie(request, response, cookieName, token, -1);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 校验用户登陆状态（鉴权），已登录返回用户信息，未登录抛出异常
     * 由客户端主动发出请求
     * 已登录用户每次访问都刷新token
     * @param token
     * @param request
     * @param response
     * @return
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(@CookieValue("LY_TOKEN") String token,
                                           HttpServletRequest request,
                                           HttpServletResponse response
                                           ){
        try {
            // 解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            // 刷新toke
            String newToken = JwtUtils.generateToken(userInfo, prop.getPrivateKey(), prop.getExpire());
            // 将新的token写入cookie
            CookieUtils.setCookie(request, response, cookieName, newToken, -1);

            // 已登录，返回用户信息
            return ResponseEntity.ok(userInfo);
        }catch (Exception e){
            // token已过期，或者token被修改
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
    }
}
