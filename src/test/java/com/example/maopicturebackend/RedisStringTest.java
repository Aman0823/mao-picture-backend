package com.example.maopicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class RedisStringTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public  void test(){
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();

        String key = "testKey";
        String value = "testValue";

//        测试新增或者更新
        valueOperations.set(key,value);
        String storedValue = valueOperations.get(key);
        assertEquals(value,storedValue,"存储值和预期不一致");

//        测试查询
        storedValue = valueOperations.get(key);
        assertEquals(value,storedValue,"查询值和预期不一致");

//        测试删除
        stringRedisTemplate.delete(key);
        storedValue = valueOperations.get(key);
        assertNull(storedValue,"删除后值不为空");

    }
}
