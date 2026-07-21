package com.seeyou.common.config;

import com.seeyou.common.utils.OssUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "com.aliyun.oss.OSS")
public class OssConfig {

    @Value("${oss.endpoint:}")
    private String endpoint;

    @Value("${oss.access-key-id:}")
    private String accessKeyId;

    @Value("${oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${oss.bucket-name:}")
    private String bucketName;

    @Bean
    public OssUtils ossUtils() {
        return new OssUtils(endpoint, accessKeyId, accessKeySecret, bucketName);
    }
}
