package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherOrderService iVoucherOrderService;
    @Resource
    RedisIdWorker redisIdWorker;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }

    @Override
    @Transactional
    public Result seckillVocher(Long voucherId) {
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
        boolean b = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .update();

        if (!b){
            return Result.fail("INSUFFICIENT STOCK");
        }
        VoucherOrder voucherOrder = new VoucherOrder();

        //Order Id
        long id = redisIdWorker.nextId("order");
        //User Id
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        //voucherID already get
        voucherOrder.setId(id);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        iVoucherOrderService.save(voucherOrder);

        return Result.ok(id);
    }
}
