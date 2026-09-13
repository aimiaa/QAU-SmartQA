package com.aimi.service;

import com.aimi.dto.user.UserLoginDTO;
import com.aimi.entity.UserEntity;
import com.aimi.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserEntity login(UserLoginDTO userLoginDTO) {

        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        UserEntity user = userMapper.getByUsername(username);

        if (user == null){
            throw new RuntimeException("用户不存在");
        }

        //对比密码
        if (!password.equals(user.getPasswordHash())) {
            //密码错误
            throw new RuntimeException("密码错误" );
        }

        return user;
    }
}
