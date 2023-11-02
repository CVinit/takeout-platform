package com.cvs.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.cvs.constant.MessageConstant;
import com.cvs.dto.UserLoginDTO;
import com.cvs.entity.User;
import com.cvs.exception.LoginFailedException;
import com.cvs.mapper.UserMapper;
import com.cvs.properties.WeChatProperties;
import com.cvs.service.UserService;
import com.cvs.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
public class UserSericeImpl implements UserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务获得微信用户的openid
        String openid = getOpenid(userLoginDTO.getCode());


        //判断openid是否为空
        if (openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否为新用户，新用户自动注册（创建user对象保存到数据库中）
        User user = userMapper.getByOpenid(openid);
        if (user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回user对象
        return user;
    }

    /**
     * 调用微信接口服务，获取微信用户openid
     * @param code
     * @return
     */
    private String getOpenid(String code){
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSONObject.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
