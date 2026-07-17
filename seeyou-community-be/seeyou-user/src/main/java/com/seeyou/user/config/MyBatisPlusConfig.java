package com.seeyou.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.seeyou.user.mapper")
public class MyBatisPlusConfig {
}