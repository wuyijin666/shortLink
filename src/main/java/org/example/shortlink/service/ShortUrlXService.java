package org.example.shortlink.service;

public interface ShortUrlXService {
    String getV1LongUrl(String shortUrl);

    String createV1ShortUrl(String longUrl);

    String getV2LongUrl(String shortUrl);

    String createV2ShortUrl(String longUrl);
}
