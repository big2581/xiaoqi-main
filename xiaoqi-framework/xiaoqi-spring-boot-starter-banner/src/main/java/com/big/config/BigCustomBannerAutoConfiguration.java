package com.big.config;


import com.big.core.BannerApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 自动装配
 *
 * @author Yin
 * @date 2024-09-24 15:11
 */
@AutoConfiguration
public class BigCustomBannerAutoConfiguration {

    @Bean
    public BannerApplicationRunner bannerApplicationRunner() {
        return new BannerApplicationRunner();
    }

}
