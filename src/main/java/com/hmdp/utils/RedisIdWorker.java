package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static java.time.LocalTime.now;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final int COUNT_BIT = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix ){

        //生成时间戳
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

        long timestamp = now-BEGIN_TIMESTAMP;
        //生成序列号
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        //拼接并返回

        return timestamp << COUNT_BIT | increment;
    }



}
