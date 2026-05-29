package com.enterprise.aiportal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.aiportal.entity.Agent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AgentMapper extends BaseMapper<Agent> {

    @Select("<script>" +
            "SELECT * FROM agents WHERE status = 1 " +
            "AND (" +
            "  visibility = 'public' " +
            "  OR creator_id = #{userId} " +
            "  <if test='deptId != null'>" +
            "    OR (visibility = 'dept' AND JSON_CONTAINS(allowed_depts, JSON_ARRAY(#{deptId}))) " +
            "  </if>" +
            "  <if test='roleIds != null and roleIds.size > 0'>" +
            "    OR (visibility = 'role' AND JSON_OVERLAPS(allowed_roles, #{roleIdsJson})) " +
            "  </if>" +
            ") " +
            "ORDER BY sort_order ASC, created_at DESC" +
            "</script>")
    List<Agent> selectVisibleAgents(@Param("userId") Long userId,
                                     @Param("deptId") Long deptId,
                                     @Param("roleIds") List<Long> roleIds,
                                     @Param("roleIdsJson") String roleIdsJson);
}
