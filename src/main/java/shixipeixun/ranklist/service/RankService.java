package shixipeixun.ranklist.service;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import shixipeixun.ranklist.dto.RankQueryDTO;
import shixipeixun.ranklist.entity.MerchantRankInfo;
import shixipeixun.ranklist.mapper.MerchantRankInfoMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RankService {

    @Resource
    private MerchantRankInfoMapper merchantRankInfoMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存相关常量
    private static final String CACHE_PREFIX = "rank:";
    private static final String CACHE_NULL = "NULL"; // 表示数据为空
    private static final String CACHE_WAITING = "WAITING"; // 表示等待数据更新
    private static final String LOCK_PREFIX = "lock:rank:";

    // 缓存时间
    private static final int CACHE_EXPIRE = 3600 * 6; // 6小时
    private static final int NULL_CACHE_EXPIRE = 300; // 空数据缓存5分钟（防穿透）
    private static final int WAITING_EXPIRE = 60; // 等待状态缓存1分钟
    private static final int LOCK_EXPIRE = 10; // 分布式锁10秒（防击穿）

    // 使用北京时间
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 获取排行榜数据
     * 1. 先查Redis缓存
     * 2. Redis没有则查数据库
     * 3. 防止缓存穿透：空结果也缓存
     * 4. 防止缓存击穿：使用分布式锁
     */
    public List<MerchantRankInfo> getRank(RankQueryDTO dto) {
        System.out.println("开始查询排行榜：" + dto);

        // 生成缓存key
        String cacheKey = generateCacheKey(dto);

        try {
            // 1. 先查Redis
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);

            if (cachedData != null) {
                System.out.println("从Redis找到缓存数据");

                if (CACHE_NULL.equals(cachedData)) {
                    // 数据是空的（防止缓存穿透）
                    System.out.println("缓存的是空数据标记，返回空列表");
                    return new ArrayList<>();
                }

                if (CACHE_WAITING.equals(cachedData)) {
                    // 数据还在等待更新
                    System.out.println("数据还在等待更新，返回空列表");
                    return new ArrayList<>();
                }

                // 正常数据
                System.out.println("返回缓存数据");
                return (List<MerchantRankInfo>) cachedData;
            }

            System.out.println("Redis没有缓存，准备查询数据库");

            // 2. Redis没有，需要查询数据库
            // 使用分布式锁防止缓存击穿（多个请求同时查询数据库）
            return getDataWithLock(dto, cacheKey);

        } catch (Exception e) {
            System.err.println("查询出错：" + e.getMessage());
            // 出错时降级到直接查数据库
            return getFromDatabaseDirectly(dto);
        }
    }

    /**
     * 使用分布式锁获取数据（防止缓存击穿）
     */
    private List<MerchantRankInfo> getDataWithLock(RankQueryDTO dto, String cacheKey) {
        String lockKey = LOCK_PREFIX + cacheKey;
        boolean gotLock = false;

        try {
            // 尝试获取分布式锁
            gotLock = tryGetLock(lockKey);

            if (gotLock) {
                System.out.println("获得分布式锁，查询数据库");
                return queryDatabaseAndCache(dto, cacheKey);
            } else {
                // 没有获取到锁，说明有其他线程正在查询
                System.out.println("没有获得锁，等待后重试缓存");

                // 等待100毫秒，让获得锁的线程完成查询
                Thread.sleep(100);

                // 再次尝试从缓存获取
                Object cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null && !CACHE_NULL.equals(cachedData) && !CACHE_WAITING.equals(cachedData)) {
                    System.out.println("重试后从缓存获取到数据");
                    return (List<MerchantRankInfo>) cachedData;
                }

                // 还是没有，返回空列表
                System.out.println("重试后缓存依然没有，返回空列表");
                return new ArrayList<>();
            }

        } catch (Exception e) {
            System.err.println("获取数据时出错：" + e.getMessage());
            return new ArrayList<>();
        } finally {
            // 释放锁
            if (gotLock) {
                releaseLock(lockKey);
            }
        }
    }

    /**
     * 查询数据库并缓存结果
     */
    private List<MerchantRankInfo> queryDatabaseAndCache(RankQueryDTO dto, String cacheKey) {
        try {
            String today = getTodayDate();
            System.out.println("查询数据库，日期：" + today);

            List<MerchantRankInfo> result = merchantRankInfoMapper.selectByConditions(
                    today, dto.getCityId(), dto.getType(), dto.getCategory());

            if (result == null || result.isEmpty()) {
                System.out.println("数据库也没有数据，缓存NULL标记（防穿透）");
                // 数据库也没有数据，缓存NULL标记，防止缓存穿透
                redisTemplate.opsForValue().set(cacheKey, CACHE_NULL, NULL_CACHE_EXPIRE, TimeUnit.SECONDS);
                return new ArrayList<>();
            }

            System.out.println("从数据库查到" + result.size() + "条数据，存入Redis");
            // 有数据，存入Redis
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE, TimeUnit.SECONDS);

            return result;

        } catch (Exception e) {
            System.err.println("查询数据库出错：" + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 尝试获取分布式锁
     */
    private boolean tryGetLock(String lockKey) {
        try {
            // 使用setIfAbsent实现分布式锁（setnx命令）
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    "locked",
                    LOCK_EXPIRE,
                    TimeUnit.SECONDS
            );
            return success != null && success;
        } catch (Exception e) {
            System.err.println("获取锁失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
            System.out.println("释放锁：" + lockKey);
        } catch (Exception e) {
            System.err.println("释放锁失败：" + e.getMessage());
        }
    }

    /**
     * 每天12点执行
     * 1. 设置重置标记
     * 2. 将所有现有缓存标记为WAITING状态
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void resetAtNoon() {
        System.out.println("[" + LocalDateTime.now(BEIJING_ZONE) + "] 中午12点，重置排行榜");

        try {
            // 获取今天的日期
            String today = getTodayDate();

            // 查找所有排行榜缓存
            Set<String> cacheKeys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (cacheKeys != null && !cacheKeys.isEmpty()) {
                // 将所有缓存标记为WAITING状态
                for (String key : cacheKeys) {
                    // 只处理今天的数据
                    if (key.contains(today)) {
                        redisTemplate.opsForValue().set(key, CACHE_WAITING, WAITING_EXPIRE, TimeUnit.SECONDS);
                    }
                }
                System.out.println("已将" + cacheKeys.size() + "个缓存标记为等待状态");
            }

            // 设置重置标记
            redisTemplate.opsForValue().set("rank:reset:" + today, "cleared", 86400, TimeUnit.SECONDS);

            System.out.println("12点重置完成");

        } catch (Exception e) {
            System.err.println("重置出错：" + e.getMessage());
        }
    }

    /**
     * 每分钟检查一次
     * 1. 检查是否已到12点后且需要更新数据
     * 2. 检查数据仓库是否有新数据
     */
    @Scheduled(fixedRate = 60000)
    public void checkEveryMinute() {
        System.out.println("[" + LocalDateTime.now(BEIJING_ZONE) + "] 每分钟检查任务开始");

        try {
            // 检查今天是否已经过了12点
            LocalDateTime now = LocalDateTime.now(BEIJING_ZONE);
            if (now.getHour() < 12) {
                // 还没到12点，不用检查
                System.out.println("还没到12点，跳过检查");
                return;
            }

            // 检查今天是否已经重置过
            String today = getTodayDate();
            Object resetFlag = redisTemplate.opsForValue().get("rank:reset:" + today);

            if (resetFlag == null) {
                // 今天还没重置过，说明还没到12点或者重置失败了
                System.out.println("今天还没重置过，跳过检查");
                return;
            }

            System.out.println("今天已重置，开始检查数据仓库");

            // 检查数据仓库是否有今天的数据
            List<MerchantRankInfo> allData = merchantRankInfoMapper.selectByDate(today);

            if (allData == null || allData.isEmpty()) {
                System.out.println("数据仓库还没有今天的数据");
                return;
            }

            System.out.println("数据仓库已有数据，共" + allData.size() + "条，开始更新缓存");

            // 数据已准备好，更新到Redis
            updateCacheWithNewData(allData, today);

        } catch (Exception e) {
            System.err.println("检查任务出错：" + e.getMessage());
        }
    }

    /**
     * 用新数据更新缓存
     */
    private void updateCacheWithNewData(List<MerchantRankInfo> allData, String date) {
        try {
            // 按条件分组
            Map<String, List<MerchantRankInfo>> groupedData = new HashMap<>();

            for (MerchantRankInfo item : allData) {
                String key = item.getCityId() + ":" + item.getType() + ":" + item.getCategory();
                if (!groupedData.containsKey(key)) {
                    groupedData.put(key, new ArrayList<>());
                }
                groupedData.get(key).add(item);
            }

            // 更新缓存（使用分布式锁防止并发问题）
            int updatedCount = 0;
            for (Map.Entry<String, List<MerchantRankInfo>> entry : groupedData.entrySet()) {
                String cacheKey = CACHE_PREFIX + entry.getKey() + ":" + date;
                String lockKey = LOCK_PREFIX + "update:" + cacheKey;

                try {
                    if (tryGetLock(lockKey)) {
                        redisTemplate.opsForValue().set(cacheKey, entry.getValue(), CACHE_EXPIRE, TimeUnit.SECONDS);
                        updatedCount++;
                        releaseLock(lockKey);
                    }
                } catch (Exception e) {
                    System.err.println("更新缓存" + cacheKey + "时出错：" + e.getMessage());
                }
            }

            System.out.println("成功更新了" + updatedCount + "个缓存");

        } catch (Exception e) {
            System.err.println("更新缓存出错：" + e.getMessage());
        }
    }

    /**
     * 直接查询数据库（降级方法）
     */
    private List<MerchantRankInfo> getFromDatabaseDirectly(RankQueryDTO dto) {
        System.out.println("降级：直接查询数据库");

        try {
            String today = getTodayDate();
            return merchantRankInfoMapper.selectByConditions(
                    today, dto.getCityId(), dto.getType(), dto.getCategory());
        } catch (Exception e) {
            System.err.println("直接查询数据库也失败了：" + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 生成缓存key
     */
    private String generateCacheKey(RankQueryDTO dto) {
        String today = getTodayDate();
        return CACHE_PREFIX + dto.getCityId() + ":" +
                dto.getType() + ":" + dto.getCategory() + ":" + today;
    }

    /**
     * 获取今天的日期（北京时间）
     */
    private String getTodayDate() {
        return LocalDate.now(BEIJING_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}