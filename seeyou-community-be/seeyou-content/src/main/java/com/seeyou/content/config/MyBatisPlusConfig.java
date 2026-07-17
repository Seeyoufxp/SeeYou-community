package com.seeyou.content.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.seeyou.content.mapper")
public class MyBatisPlusConfig {
}