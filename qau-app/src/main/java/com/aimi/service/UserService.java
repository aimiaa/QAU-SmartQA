package com.aimi.service;

import com.aimi.dto.user.UserLoginDTO;
import com.aimi.entity.UserEntity;

public interface UserService {

    /**
     * 用户登录
     * @param userLoginDTO
     * @return
     */
    UserEntity login(UserLoginDTO userLoginDTO);
}
