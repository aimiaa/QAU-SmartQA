package com.aimi.mapper;

import com.aimi.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            select *
            from user_account
            where username = #{username}
              and deleted = false
            limit 1
            """)
    UserEntity getByUsername(@Param("username") String username);

    @Update("update user_account set password_hash = #{hash} where id = #{id}")
    int updatePasswordHash(@Param("id") Long id, @Param("hash") String hash);
}
