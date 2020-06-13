package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "user:verify:phone:";


    public Boolean checkData(String data, Integer type) {
        User record = new User();
        switch (type) {
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }
        return userMapper.selectCount(record) == 0;
    }

    public void sendCode(String phone) {
        String key = KEY_PREFIX + phone;
        // 生成验证码
        String code = NumberUtils.generateCode(6);
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend("ly.sms.exchange", "sms.verify.code", msg);
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }

    public Boolean register(User user, String code) {
        String key = KEY_PREFIX + user.getPhone();

        // 检验短信验证码
        String cacheCode = redisTemplate.opsForValue().get(key);
        if (!StringUtils.equals(cacheCode, code)) {
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }

        // 生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);

        // 对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        user.setId(null);
        user.setCreated(new Date());

        Boolean boo = userMapper.insertSelective(user) == 1;
        if(boo){
            redisTemplate.delete(key);
        }
        return null;
    }

    public User query(String username, String password) {

        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);
        // 校验用户名
        if (user == null) {
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        // 检验密码
        if (!StringUtils.equals(user.getPassword(), CodecUtils.md5Hex(password, user.getSalt()))) {
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        // 用户名和密码正确
        return user;
    }
}
