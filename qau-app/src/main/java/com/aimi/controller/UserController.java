package com.aimi.controller;

import com.aimi.dto.user.UserLoginDTO;
import com.aimi.entity.UserEntity;
import com.aimi.result.Result;
import com.aimi.security.JwtTokenProvider;
import com.aimi.service.UserService;
import com.aimi.vo.user.UserLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/user")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        UserEntity user = userService.login(userLoginDTO);
        String token = jwtTokenProvider.generateToken(user);

        UserLoginVO vo = UserLoginVO.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .realName(user.getRealName())
                .token(token)
                .build();

        return Result.success(vo);
    }
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
