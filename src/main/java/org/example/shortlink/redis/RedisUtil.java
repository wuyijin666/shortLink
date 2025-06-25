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
   public Long incr (String key, int delta){
       if(delta < 0) {
           throw new RuntimeException("递增因子必须大于0");
       }
       return redisTemplate.opsForValue().increment(key, 1);
   }



    /**
     * 普通缓存获取
     * @param key longUrl 键
     * @return
     */
    public Object get(String key) {
        return  key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     * @param  key longUrl 键
     * @param value shortUrl 值
     */
    public boolean set(String key, Object value) {
       try{
           redisTemplate.opsForValue().set(key, value);
           return true;
       }catch (Exception e){
           e.printStackTrace();
           return false;
       }
    }

    public List<Object> executeLua(String redisScript, List<String> keys, List<Object> values){
        try{
            List<Object> result = redisTemplate.execute(new DefaultRedisScript<>(redisScript, List.class), keys, values);
            System.out.println("result =  " + result);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }


    }


}
