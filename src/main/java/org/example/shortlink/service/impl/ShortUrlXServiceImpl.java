package org.example.shortlink.service.impl;

import org.example.shortlink.mapper.UrlMapMapper;
import org.example.shortlink.model.UrlMap;
import org.example.shortlink.redis.RedisUtil;
import org.example.shortlink.service.ShortUrlXService;
import org.example.shortlink.utils.Base62;
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

    @Override
    public String getV1LongUrl(String shortUrl) {
        // 直接从数据库中获取
        return urlMapMapper.doGetLongUrl(shortUrl);
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
    public String getV2LongUrl(String shortUrl) {
        return null;
    }

    @Override
    public String createV2ShortUrl(String longUrl) {
        // 1. 先查db
        String shortUrl = urlMapMapper.doGetShortUrl(longUrl);
        if(shortUrl != null && !shortUrl.isEmpty()) {
            return shortUrl;
        }
        // 若没有
        // 2. INCR请求redis
        Long id = redisUtil.incr(cacheIdKey, 1);
        // 3. base62通过id生成shortlink
        shortUrl = base62.generateShortUrl(id);
        // 4.将生成的shortlink保存到布隆过滤器
        addShortUrlToBloomFilterLua(shortUrl);
        return shortUrl;
    }

    private void addShortUrlToBloomFilterLua(String shortUrl) {
        List<String> keys = new ArrayList<>();
        keys.add(shortUrlBloomFilterKey);
        List<Object> values = new ArrayList<>();
        values.add(shortUrl);
        redisUtil.executeLua(addShortUrlToBloomFilterLua, keys, values);
    }

    private final String projectPrefix = "shortUrlX-";
    private final String cacheIdKey = projectPrefix + "IncrId";
    private final String shortUrlBloomFilterKey = projectPrefix + "BloomFilter-ShortUrl";
    private final String addShortUrlToBloomFilterLua = "redis.call('bf.add', KEYS[1], ARGV[1])";

}
