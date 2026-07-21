package com.seeyou.chat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.seeyou.chat.mapper")
public class MyBatisPlusConfig {
}