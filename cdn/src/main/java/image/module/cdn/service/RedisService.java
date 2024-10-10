package image.module.cdn.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 값을 Redis에 저장하는 메서드
    public void setValue(String key, String value, int cachingTime) {
        redisTemplate.opsForValue().set(key + ":ttl", String.valueOf(cachingTime));
        redisTemplate.opsForValue().set(key, value, cachingTime, TimeUnit.MINUTES);
    }

    // 값을 Redis에서 가져오는 메서드
    public String getValue(String key) {
        String initialTTL = redisTemplate.opsForValue().get(key + ":ttl");
        redisTemplate.expire(key, Integer.parseInt(initialTTL), TimeUnit.MINUTES);
        return redisTemplate.opsForValue().get(key);
    }
}