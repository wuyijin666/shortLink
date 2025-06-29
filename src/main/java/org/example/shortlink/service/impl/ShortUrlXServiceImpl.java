package org.example.shortlink.service.impl;

import org.example.shortlink.mapper.UrlMapMapper;
import org.example.shortlink.model.UrlMap;
import org.example.shortlink.redis.RedisUtil;
import org.example.shortlink.service.ShortUrlXService;
import org.example.shortlink.utils.Base62;
import org.example.shortlink.utils.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShortUrlXServiceImpl implements ShortUrlXService {
    @Autowired
    private UrlMapMapper urlMapMapper;
    @Autowired
    private Base62 base62;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @Override
    public String getV1LongUrl(String shortUrl) {
        // 直接从数据库中获取
        return urlMapMapper.doGetLongUrl(shortUrl);
    }

    @Override
    public String getV2LongUrl(String shortUrl) {
        // 1. 先从布隆过滤器中取
        List<String> keys = new ArrayList<>();
        keys.add(shortUrlBloomFilterKey);
        List<Object> values = new ArrayList<>();
        values.add(shortUrl);
        List<Object> result = redisUtil.executeLua(findShortUrlFromBloomFilterLua, keys, values);
        System.out.println("result = " + result);
        assert result != null;
        long isExist = (long) result.get(0);
        if(isExist == 1){
            // 2. 如果存在，还得从db中取
            return urlMapMapper.doGetLongUrl(shortUrl);
        }
        // 布隆过滤器不存在，直接返回空
        return null;
    }

    @Override
    public String getV3LongUrl(String shortUrl) {
        // 从布隆过滤器中查询以及从缓存中查询
        List<String> keys = new ArrayList<>();
        keys.add(shortUrlBloomFilterKey);
        keys.add(shortUrlPrefix + shortUrl);
        List<Object> values = new ArrayList<>();
        values.add(shortUrl);
        List<Object> result = redisUtil.executeLua(findShortUrlInBloomFilterAndCacheLua, keys, values);
        assert result != null;
        long need = (long) result.get(0);
        if(need == 1){
            return urlMapMapper.doGetLongUrl(shortUrl);
        }
        return (String) result.get(1);
    }


    @Override
    public String createV1ShortUrl(String longUrl) {
      // 1. 先从db中获取到
        String shortUrl = urlMapMapper.doGetShortUrl(longUrl);
        if(shortUrl != null && !shortUrl.isEmpty()) {
            return shortUrl;
        }
        // 若没有，在db中创建一条记录
        UrlMap urlMap = new UrlMap(longUrl);
        urlMapMapper.dbCreate(urlMap);
        // 利用base62算法 生成短链
        shortUrl = base62.generateShortUrl(urlMap.getId());

        // 更新db中的短链
        urlMapMapper.doUpdate(shortUrl, urlMap.getLongUrl());
        return shortUrl;
    }

    @Override
    public String createV2ShortUrl(String longUrl) {
        // 1. 先查db
        String shortUrl = urlMapMapper.doGetShortUrl(longUrl);
        if(shortUrl != null && !shortUrl.isEmpty()) {
            return shortUrl;
        }
        // 若没有
        // 2. INCR请求redis，获取自增id
        Long id = redisUtil.incr(cacheIdKey, 1);
        // 3. base62通过id生成shortlink
        shortUrl = base62.generateShortUrl(id);
        // 4. 将生成的shortUrl保存到布隆过滤器
        addShortUrlToBloomFilterLua(shortUrl);
        // 5. 保存到db中
        urlMapMapper.dbCreate(new UrlMap(longUrl, shortUrl));
        return shortUrl;
    }

    @Override
    public String createV3ShortUrl(String longUrl) {
        // 1. 先查询缓存里是否有长链对应的短链
        String shortUrl = (String) redisUtil.get(longUrlPrefix + longUrl);
        // 2. 如果有，直接返回该短链
        if(shortUrl != null && !shortUrl.isEmpty()){
            return shortUrl;
        }
        // 3. 缓存没有，去db中查询是否有该长链对应的短链
        shortUrl = urlMapMapper.doGetShortUrl(longUrl);
        // 4. db中有，就更新到缓存中
        if (shortUrl != null && !shortUrl.isEmpty()){
            redisUtil.set(longUrlPrefix + longUrl, shortUrl);
            return shortUrl;
        }
        // 5. db中没有，就利用雪花算法生成ID
        Long id = snowflakeIdWorker.nextId();
        // 6. 利用base62算法，生成短链
        shortUrl = base62.generateShortUrl(id);
        // 7. 将短链保存到布隆过滤器中
        addShortUrlToBloomFilterLua(shortUrl);
        // 8. 将短链保存到缓存中，以便下次查询
        redisUtil.set(longUrlPrefix + longUrl, shortUrl);
        // 9. 保存db中
        urlMapMapper.dbCreate(new UrlMap(longUrl, shortUrl));
        return shortUrl;
    }


    private void addShortUrlToBloomFilterLua(String shortUrl) {
        List<String> keys = new ArrayList<>();
        keys.add(shortUrlBloomFilterKey);
        List<Object> values = new ArrayList<>();
        values.add(shortUrl);
        redisUtil.executeLua(addShortUrlToBloomFilterLua, keys,values);
    }

    private final String projectPrefix = "shortUrlX-";
    private final String shortUrlPrefix = projectPrefix + "ShortUrl:";
    private final String longUrlPrefix = projectPrefix + "LongUrl:";
    private final String cacheIdKey = projectPrefix + "IncrId";
    private final String shortUrlBloomFilterKey = projectPrefix + "BloomFilter-ShortUrl";
    private final String findShortUrlFromBloomFilterLua = "local exist = redis.call('bf.exists', KEYS[1], ARGV[1])\n" +
            "return exist\n";
    private final String addShortUrlToBloomFilterLua = "redis.call('bf.add', KEYS[1], ARGV[1])";
    // 本lua脚本实现两级缓存查询策略，用于优化短连接服务的查询性能
    private final String findShortUrlInBloomFilterAndCacheLua = "local bloomKey = KEYS[1]\nlocal cacheKey = KEYS[2]\nlocal bloomVal = ARGV[1]\n\n-- 检查val是否存在于布隆过滤器对应的bloomKey中\nlocal exist = redis.call('bf.exists', bloomKey, bloomVal)\n\n-- 如果bloomVal不存在于布隆过滤器，直接返回空字符串，返回0代表不需要查询db了\nif exist == 0 then\n   return {0, ''}\nend\n\n-- 如果bloomVal存在于布隆过滤器，查询cacheKey\nlocal value = redis.call('GET', cacheKey)\n\n-- 如果cacheKey存在，就返回对应的值，否则返回空字符串\nif value then\n return {0, value}\nelse\n return {1, ''}\nend";


}
