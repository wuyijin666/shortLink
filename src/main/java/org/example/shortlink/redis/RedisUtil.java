package org.example.shortlink.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     * @return
     */
    public Long incr(String key, long delta) {
        if(delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public List<Object> executeLua(String redisScript, List<String> keys, List<Object> values) {
        try{
            List<Object> result = redisTemplate.execute(new DefaultRedisScript<>(redisScript, List.class), keys, values);
            System.out.println("result = " + result);
            return result;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }
}
