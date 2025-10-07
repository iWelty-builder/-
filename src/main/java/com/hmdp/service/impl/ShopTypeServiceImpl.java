package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.security.Key;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private IShopTypeService typeService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result rqurey() {


        String Json = stringRedisTemplate.opsForValue().get("cache:shop:type");
        if (StrUtil.isNotBlank(Json)){
            List<ShopType> shopTypeList = JSONUtil.toList(JSONUtil.parseArray(Json), ShopType.class);
            return Result.ok(shopTypeList);
        }

        List<ShopType> typeList = typeService.query().orderByAsc("sort").list();

        if (typeList.isEmpty()){
            return Result.fail("TYPE IS EMPTY");
        }

        stringRedisTemplate.opsForValue().set("cache:shop:type",JSONUtil.toJsonStr(typeList));

        return Result.ok(typeList);
    }
}
