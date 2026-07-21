package com.seeyou.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Slf4j
public class OssUtils {

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;
    private OSS ossClient;

    public OssUtils(String endpoint, String accessKeyId, String accessKeySecret, String bucketName) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
    }

    private OSS getClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                }
            }
        }
        return ossClient;
    }

    public String upload(InputStream inputStream, String objectKey, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        getClient().putObject(bucketName, objectKey, inputStream, metadata);
        return "https://" + bucketName + "." + endpoint + "/" + objectKey;
    }

    public String upload(byte[] bytes, String objectKey, String contentType) {
        return upload(new ByteArrayInputStream(bytes), objectKey, contentType);
    }

    public InputStream download(String objectKey) {
        return getClient().getObject(bucketName, objectKey).getObjectContent();
    }

    public void delete(String objectKey) {
        getClient().deleteObject(bucketName, objectKey);
    }

    public String getSignedUrl(String objectKey, long expireMillis) {
        URL url = getClient().generatePresignedUrl(bucketName, objectKey,
                new Date(System.currentTimeMillis() + expireMillis));
        return url.toString();
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS client shutdown");
        }
    }
}
