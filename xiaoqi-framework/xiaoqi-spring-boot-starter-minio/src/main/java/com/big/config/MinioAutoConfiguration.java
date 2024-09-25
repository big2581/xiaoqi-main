package com.big.config;

import com.big.core.MinioClientUtil;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 自动装配minio 客户端
 *
 * @author Yin
 * @date 2024-09-25 9:10
 */
@AutoConfiguration
@EnableConfigurationProperties(MinioConfig.class)
public class MinioAutoConfiguration {

    @Autowired
    private MinioConfig minioConfig;

    @Bean
    @ConditionalOnClass(MinioConfig.class)
    public MinioClientUtil minioClientUtil() {
        MinioClient minioClient = new MinioClient.Builder()
                .endpoint(minioConfig.getEndpoint(), minioConfig.getPort(), minioConfig.getSecure())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey()).build();
        return new MinioClientUtil(minioClient);
    }
}
