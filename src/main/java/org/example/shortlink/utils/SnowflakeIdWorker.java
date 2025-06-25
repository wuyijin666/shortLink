package org.example.shortlink.utils;

import jakarta.annotation.PostConstruct;
import org.example.shortlink.common.conf.AppConf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdWorker {

    @Autowired
    private AppConf appConf;

    // private int workerId;
    // 工作机器ID（默认0～1023）
    private long workerId;
    // 序列号（1ms内默认0～4095）
    private long sequence = 0L;

    // 开始时间戳
    private long twepoch = System.currentTimeMillis();

    // workerId位数
    private long workerIdBits = 10L;
    // 最大值
    private long maxWorkerId;

    private long sequenceBits = 12L;

    // workerId左移12位
    private long workerIdShift = sequenceBits;
    // 时间戳左移22位(10+12)
    private long timestampLeftShift;
    // 生成序列的掩码
    private long sequenceMask = ~(-1L << sequenceBits);

    private long lastTimestamp = -1L;

    @PostConstruct
    public void init(){
        // 获取work_id
        workerId = appConf.getWorkId();
        // 获取workerIdBits
        workerIdBits = appConf.getWorkerIdBits();
        maxWorkerId  = ~(-1L << workerIdBits);
        timestampLeftShift = sequenceBits + workerIdBits;
        sequence = 0L;
        // 做校验
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        } else {
            System.out.println("workerId: " + workerId);
        }

        if  (workerIdBits > 12 || workerIdBits < 0) {
            throw new IllegalArgumentException("worker Id bits can't be greater than 12 or less than 0");
        } else {
            System.out.println("workerIdBits: " + workerIdBits);
        }

        System.out.println("InitByPostConstructAnnotation do something");
    }

    /**
     * 通过雪花算法生成下一个id，注意这里使用synchronized同步
     *
     * @return 唯一id
     */
    public synchronized long nextId() {

        long timestamp = timeGen();

        // 当前时间小于上一次生成id使用的时间，可能出现服务器时钟回拨问题
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            // 序列号的最大值是4095，使用掩码（最低12位为1，高位都为0）进行位与运行后如果值为0，则自增后的序列号超过了4095
            // 那么就使用新的时间戳
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else { // 时间戳改变，毫秒内序列重置
            sequence = 0;
        }

        // 记录最后一次使用的毫秒时间戳
        lastTimestamp = timestamp;

        // 核心算法，移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift) | sequence;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }


    private long tilNextMillis(long lastTimeMillis) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

//    public static void main(String[] args) {
//        SnowflakeIdWorker snowflakeIdGenerator = new SnowflakeIdWorker();
//        for (int i = 0; i < 1000; i++) {
//            System.out.println(snowflakeIdGenerator.nextId());
//        }
//    }
 }
