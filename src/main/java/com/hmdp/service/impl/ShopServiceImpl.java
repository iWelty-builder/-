package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {

        Shop shop = queryWithPenetration(id);

        return Result.ok(shop);
    }

    public Shop queryWithPenetration(Long id){
        String key = CACHE_SHOP_KEY + id;

        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if (!StrUtil.isBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        if (shopJson != null){
            return null;
        }

        Shop byId = getById(id);

        if (byId == null) {
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(byId),CACHE_SHOP_TTL,TimeUnit.MINUTES);

        return byId;
    }

    public Shop queryWithBreakDown(Long id) throws InterruptedException {
        String key = CACHE_SHOP_KEY + id;

        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if (!StrUtil.isBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        if (shopJson != null){
            return null;
        }

        String lockKey = "lock:shop:" + id;

        boolean triedLock = tryLock(lockKey);
        if (!triedLock){
            Thread.sleep(50);
            return queryWithBreakDown(id);
        }

        Shop byId = getById(id);

        if (byId == null) {
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(byId),CACHE_SHOP_TTL,TimeUnit.MINUTES);

        unLock(lockKey);

        return byId;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    public void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    public Result update(Shop shop) {
        if (shop.getId() == null){
            return Result.fail("THIS SHOP ISN'T EXIST , ID CAN'T BE NULL");
        }

        updateById(shop);

        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
