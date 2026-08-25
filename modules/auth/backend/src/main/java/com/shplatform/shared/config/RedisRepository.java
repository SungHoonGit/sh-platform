package com.shplatform.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisRepository.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis SET failed for key={}: {}", key, e.getMessage());
        }
    }

    public String get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Redis GET failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Redis HASKEY failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Redis INCR failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.warn("Redis EXPIRE failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis EXPIRE failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public void hashPut(String key, String hashKey, String value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            log.warn("Redis HPUT failed for key={}: {}", key, e.getMessage());
        }
    }

    public Object hashGet(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.warn("Redis HGET failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public void hashDelete(String key, String... hashKeys) {
        try {
            redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
        } catch (Exception e) {
            log.warn("Redis HDELETE failed for key={}: {}", key, e.getMessage());
        }
    }

    public Long addToSet(String key, String... values) {
        try {
            return redisTemplate.opsForSet().add(key, (Object[]) values);
        } catch (Exception e) {
            log.warn("Redis SADD failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Long removeFromSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.warn("Redis SREM failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Set<Object> getSetMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.warn("Redis SMEMBERS failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Long getSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.warn("Redis SCARD failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Long leftPush(String key, String value) {
        try {
            return redisTemplate.opsForList().leftPush(key, value);
        } catch (Exception e) {
            log.warn("Redis LPUSH failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public Collection<Object> getList(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.warn("Redis LRANGE failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }
}
