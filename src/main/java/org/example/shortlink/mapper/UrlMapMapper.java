package org.example.shortlink.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.shortlink.model.UrlMap;

@Mapper
public interface UrlMapMapper {
    void dbCreate(@Param("urlMap") UrlMap urlMap);

    String doGetLongUrl(@Param("longUrl") String longUrl);
    String doGetShortUrl(@Param("shortUrl") String shortUrl);
    void doUpdate(@Param("shortUrl") String shortUrl, @Param("longUrl") String longUrl);


}
