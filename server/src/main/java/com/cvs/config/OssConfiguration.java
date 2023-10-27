package com.cvs.config;

import com.cvs.properties.S3OssProperties;
import com.cvs.utils.S3OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OssConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public S3OssUtil s3OssUtil(S3OssProperties s3OssProperties){
        log.info("开始创建backblaze文件上传工具类对象:{}",s3OssProperties);
        return new S3OssUtil(s3OssProperties.getEndpoint(),
                s3OssProperties.getAccessKeyId(),
                s3OssProperties.getAccessKeySecret(),
                s3OssProperties.getBucketName(),s3OssProperties.getObjectPath());
    }
}
