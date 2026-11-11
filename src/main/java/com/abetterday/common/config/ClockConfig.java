package com.abetterday.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 시간대를 한 곳에 모으는 설정. 모든 날짜·시간 계산은 이 Clock 빈을 주입받아 사용한다. */
@Configuration
public class ClockConfig {

    /** Asia/Seoul 기준 Clock 빈을 생성한다. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
