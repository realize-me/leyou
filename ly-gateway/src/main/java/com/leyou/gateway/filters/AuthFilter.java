package com.leyou.gateway.filters;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter {
    @Autowired
    private JwtProperties prop;
    @Autowired
    private FilterProperties filterProp;

    @Override
    public String filterType() {
        //  过滤器类型：前置过滤器
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        // 过滤器的顺序：官方过滤器之前
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        // 获取上下文
        RequestContext context = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = context.getRequest();
        // 获取url路径
        String path = request.getRequestURI();
        // url路径在白名单返回true，路径没在白名单返回false
        Boolean allow = isAllowPath(path);

        // true表示拦截，false表示放行
        return !allow;
    }

    // url路径在白名单返回true，路径没在白名单返回false
    private Boolean isAllowPath(String path) {
        for (String allowPath : filterProp.getAllowPaths()) {
            if (path.startsWith(allowPath)){
                return true;
            }
        }
        return false;
    }


    @Override
    public Object run() throws ZuulException {
        // 获取上下文
        RequestContext context = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = context.getRequest();
        // 获取cookie中的token
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        try {
            // 解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            // TODO 校验权限
        } catch (Exception e) {
            // 解析token失败，表示未登录，进行拦截
            context.setSendZuulResponse(false);
            // 返回状态码
            context.setResponseStatusCode(403);
        }

        return null;
    }
}
