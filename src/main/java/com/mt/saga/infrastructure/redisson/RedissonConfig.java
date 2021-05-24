package com.mt.saga.infrastructure.redisson;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient configRedisson() {
        log.debug("start of configure redisson");
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6378");
        RedissonClient redisson = Redisson.create(config);
        return redisson;

    }
}
