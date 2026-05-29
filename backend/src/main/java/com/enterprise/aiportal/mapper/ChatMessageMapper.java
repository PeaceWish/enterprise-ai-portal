package com.enterprise.aiportal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.aiportal.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {}