package com.enterprise.aiportal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.aiportal.entity.Department;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DepartmentMapper extends BaseMapper<Department> {

    @Select("SELECT * FROM departments WHERE dept_code = #{deptCode} LIMIT 1")
    Department selectByDeptCode(@Param("deptCode") String deptCode);
}
