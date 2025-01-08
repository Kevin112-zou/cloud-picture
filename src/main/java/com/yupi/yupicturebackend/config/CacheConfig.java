package com.yupi.yupicturebackend.config;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;


@Configuration
public class CacheConfig {

    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_1000)
                .expireAfterAccess(5, TimeUnit.SECONDS));
        return cacheManager;
    }

    /* 如果配置refreshAfterWrite要有这个bean，否则报错
    java.lang.IllegalStateException: refreshAfterWrite requires a LoadingCache*/
    @Bean
    public CacheLoader<Object, Object> cacheLoader() {
        CacheLoader<Object, Object> cacheLoader = new CacheLoader<Object, Object>() {
            @Nullable
            @Override
            public Object load(@NonNull Object key) throws Exception {
                return null;
            }
        };
        return cacheLoader;
    }

}


