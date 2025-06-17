package org.example.shortlink.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.shortlink.common.conf.BaseModel;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class UrlMap extends BaseModel implements Serializable {
    private Long id;
    private String longUrl;
    private String shortUrl;
    private Date createAt;

    @Override
    public String toString() {
        return "UrlMap{" +
                "id='" + id + '\'' +
                ", longUrl='" + longUrl + '\'' +
                ", shortUrl='" + shortUrl + '\'' +
                ", createAt=" + createAt +
                '}';
    }

    public UrlMap(String longUrl) {
        this.longUrl = longUrl;
    }

    public UrlMap(String longUrl, String shortUrl) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
    }
}
