package com.leyou.cart.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.pojo.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.bind.util.JAXBSource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "cart:uid:";
    public void addCart(Cart cart) {
        // 获取登陆用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        // hash结构的key
        String key = KEY_PREFIX + userInfo.getId();
        // hash结构的hashKey
        String hashKey = cart.getSkuId().toString();
        // 保存原有数量
        Integer num = cart.getNum();
        // 对hash结构的操作绑定了key
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);
        if (operation.hasKey(hashKey)) {
            // redis中用户的购物车中已存在这个商品
            // 获取hashValue
            String json = operation.get(hashKey).toString();
            // json转成对象
            cart = JsonUtils.parse(json, Cart.class);
            // 商品数量在原来数量基础上增加
            cart.setNum(cart.getNum() + num);
        }
        // 写回redis
        operation.put(hashKey, JsonUtils.serialize(cart));

    }

    public List<Cart> queryCartList() {
        // 获取登陆用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        // hash结构的key
        String key = KEY_PREFIX + userInfo.getId();
        // 购物车为空
        if (!redisTemplate.hasKey(key)) {
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        // 对hash结构的操作绑定了key，获取指定用户的购物车的操作
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);
        // 获取购物车中所有的商品（json list），并转成Cart对象（cart list）
        List<Cart> cartList = operation.values().stream().map(o -> JsonUtils.parse(o.toString(), Cart.class)).collect(Collectors.toList());

        return cartList;
    }


    public void updateCartNum(Long skuId, Integer num) {
        // 获取登陆用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        // hash结构的key
        String key = KEY_PREFIX + userInfo.getId();
        // 对hash结构的操作绑定了key，获取指定用户的购物车的操作
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);
        // 获取hashKey
        String hashKey = skuId.toString();
        // 购物车中不存在这个商品
        if (!operation.hasKey(hashKey)) {
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        // 查询商品并转成cart对象
        Cart cart = JsonUtils.parse(operation.get(hashKey).toString(), Cart.class);
        // 更新商品数量
        cart.setNum(num);
        // 写回redis
        operation.put(hashKey, JsonUtils.serialize(cart));
    }

    public void deleteCart(Long skuId) {
        // 获取登陆用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        // hash结构的key
        String key = KEY_PREFIX + userInfo.getId();
        // 删除sku
        redisTemplate.opsForHash().delete(key, skuId.toString());
    }
}
