package com.aimi.service.impl;

import com.aimi.dto.user.UserLoginDTO;
import com.aimi.entity.UserEntity;
import com.aimi.mapper.UserMapper;
import com.aimi.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserEntity login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String rawPassword = userLoginDTO.getPassword();

        UserEntity user = userMapper.getByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String stored = user.getPasswordHash();
        boolean ok;

        if (stored != null && stored.startsWith("$2")) {
            // 已是 BCrypt 哈希：正常比对
            ok = passwordEncoder.matches(rawPassword, stored);
        } else {
            // 兼容历史明文数据：先明文比对，成功后立即升级为 BCrypt 哈希
            ok = stored != null && stored.equals(rawPassword);
            if (ok) {
                userMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(rawPassword));
            }
        }

        if (!ok) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }
}
