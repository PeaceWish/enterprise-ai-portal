package com.enterprise.aiportal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.aiportal.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT u.*, d.dept_name FROM users u LEFT JOIN departments d ON u.dept_id = d.id WHERE u.username = #{username} AND u.status = 1")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT u.*, d.dept_name FROM users u LEFT JOIN departments d ON u.dept_id = d.id WHERE u.user_code = #{userCode} LIMIT 1")
    User selectByUserCode(@Param("userCode") String userCode);
}
