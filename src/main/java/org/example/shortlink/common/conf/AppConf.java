package org.example.shortlink.common.conf;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppConf {
    @Value("1")
    private int workId;

    @Value("10")
    private int workerIdBits;
}
