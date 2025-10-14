package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherOrderService iVoucherOrderService;
    @Resource
    RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;















    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("FLASH SALE HAVEN'T START !!!");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("FLASH SALE HAVE END !!!");
        }

        //Judge whether the goods are haven't sold out.
        if (!(seckillVoucher.getStock() > 1)) {
            return Result.fail("SORRY THIS VOUCHER HAVE SOLD OUT !!!");
        }
//        seckillVoucher.setStock(seckillVoucher.getStock() - 1);
//        seckillVoucher.setUpdateTime(LocalDateTime.now());
//        seckillVoucherService.updateById(seckillVoucher);
        Long userId = UserHolder.getUser().getId();
//        synchronized (userId.toString().intern()) {
//            //添加AspectJ Weaver 依赖 并在启动类上添加@EnableAspectJAutoProxy注解，并将exposeProxy属性设置为true
//            //获取代理对象（事务）
//        SimpleRedisLock lock = new SimpleRedisLock("order" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order" + userId);

        boolean isLock = lock.tryLock();

        if (!isLock) {
            return Result.fail("One Person One Flash Sale Voucher");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
//        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //一人一单
        UserDTO user = UserHolder.getUser();
        Long userId1 = user.getId();

        //查询订单
        int count = iVoucherOrderService.query().eq("user_id", userId1).eq("voucher_id", voucherId).count();
        //判断用户id是否在订单表中出现过
        if (count > 0) {
            //出现过，返回不允许重复抢购
            return Result.fail("不允许重复抢购");
        }

        boolean b = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();

        if (!b) {
            return Result.fail("INSUFFICIENT STOCK");
        }

        VoucherOrder voucherOrder = new VoucherOrder();

        //Order Id
        long id = redisIdWorker.nextId("order");
        //User Id

        Long userId = user.getId();
        //voucherID already get
        voucherOrder.setId(id);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        iVoucherOrderService.save(voucherOrder);

        return Result.ok(id);

    }
}
