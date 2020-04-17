package com.qingcheng.service.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * Redis中获取秒杀商品列表 TimeSlot:时间段
     * @param key
     * @return
     */
    @Override
    public List<SeckillGoods> findListByTimeSlot(String key) {
        return  redisTemplate.boundHashOps("SeckillGoods_"+key).values();
    }

    /**
     * 根据商品id和秒杀开始时间查询秒杀商品 直接从缓存中查询
     * @param time 秒杀开始时间
     * @param id 商品id
     * @return
     */
    @Override
    public SeckillGoods getOne(String time, Long id) {
        return (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+time).get(id);
    }
}
