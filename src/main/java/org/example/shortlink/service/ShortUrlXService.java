package org.example.shortlink.service;

public interface ShortUrlXService {
    String getV1LongUrl(String shortUrl);

    String createV1ShortUrl(String longUrl);

}
