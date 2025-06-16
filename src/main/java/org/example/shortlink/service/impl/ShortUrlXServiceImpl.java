package org.example.shortlink.service.impl;

import org.example.shortlink.mapper.UrlMapMapper;
import org.example.shortlink.model.UrlMap;
import org.example.shortlink.service.ShortUrlXService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShortUrlXServiceImpl implements ShortUrlXService {
    @Autowired
    private UrlMapMapper urlMapMapper;
    @Autowired
    private Base62 base62;

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
        shortUrl = base62.encode(urlMap.getId());


    }
}
