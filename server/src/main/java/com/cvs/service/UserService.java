package com.cvs.service;

import com.cvs.dto.UserLoginDTO;
import com.cvs.entity.User;

public interface UserService {

    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
