package com.enterprise.aiportal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.entity.Department;
import com.enterprise.aiportal.mapper.DepartmentMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/depts")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class DeptAdminController {

    private final DepartmentMapper departmentMapper;

    public DeptAdminController(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @GetMapping
    public Result<List<Department>> list(@RequestParam(defaultValue = "false") Boolean onlyEnabled) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getSortOrder);
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(Department::getStatus, 1);
        }
        List<Department> list = departmentMapper.selectList(wrapper);
        return Result.ok(list);
    }

    @PostMapping
    public Result<String> create(@RequestBody Department dept) {
        dept.setSource("local");
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(dept);
        return Result.ok("创建成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Department dept) {
        dept.setId(id);
        dept.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(dept);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        departmentMapper.deleteById(id);
        return Result.ok("删除成功");
    }
}
