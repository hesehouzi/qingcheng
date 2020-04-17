package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {

    /**
     * 通过时间段查询对应的秒杀商品集合
     * @param key
     * @return
     */
    public List<SeckillGoods> findListByTimeSlot(String key);

    /**
     * 根据商品id和秒杀开始时间查询秒杀商品
     * @param time 秒杀开始时间
     * @param id 商品id
     * @return
     */
    public SeckillGoods getOne(String time,Long id);

}
