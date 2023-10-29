package com.cvs.interceptor;

import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class ShopStatusInterceptor implements HandlerInterceptor {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("根据店铺状态拦截用户订单...");
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String shopStatus = (String) valueOperations.get("SHOP_STATUS");
        Integer status = Integer.valueOf(shopStatus);
        if (status.equals(0) && request.getPathInfo().contains("/user/order")) {
            log.error("店铺已关闭，下单请求拦截...");
            return false;
        }
        return true;
    }
}
