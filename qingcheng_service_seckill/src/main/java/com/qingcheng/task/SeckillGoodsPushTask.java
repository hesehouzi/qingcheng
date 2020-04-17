package com.qingcheng.task;

import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;


import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 定时任务操作
 */
@Component//被spring管理
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 定时任务方法 将数据库中的秒杀商品信息查询得到并存入缓存中
     * 0/30 * * * * ? :从每分钟的第0秒开始执行 每过30s执行一次
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void loadGoodsPushRedis(){
        //1.获取时间段集合
        List<Date> dateMenus = DateUtil.getDateMenus();
        //2.循环时间段
        for (Date startTime : dateMenus) {
            //namespace = SeckillGoods_20200323
            String extName = DateUtil.date2Str(startTime);
            //3.根据时间段数据查询对应的秒杀商品数据
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            //1）商品必须通过审核 status=1
            criteria.andEqualTo("status","1");
            //2）库存>0
            criteria.andGreaterThan("stockCount","0");
            //3） 秒杀开始时间<=商品开始时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            //4）商品结束时间<秒杀开始时间+2小时
            criteria.andLessThan("endTime",DateUtil.addDateHour(startTime,2));
            //5）排除之前在redis中已经存在的数据
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + extName).keys();
            if (keys!=null&&keys.size()>0){
                criteria.andNotIn("id",keys);//条件为 查询的id字段不含keys
            }
            //6）查询数据
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(example);
            //4.将秒杀商品数据存入到redis中
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                redisTemplate.boundHashOps("SeckillGoods_"+extName).put(seckillGoods.getId(),seckillGoods);
                //将商品个数压入到Redis队列中
                Long[] ids = pushId(seckillGoods.getStockCount(), seckillGoods.getId());
                redisTemplate.boundListOps("SeckillGoodsList_"+seckillGoods.getId()).leftPushAll(ids);
                //将商品个数压入到Redis中，记录商品个数 保证商品个数精准判断
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(),seckillGoods.getStockCount());
            }
        }
    }

    /**
     * 根据商品的个数 将商品id组成一个数组
     * @param len 长度
     * @param id 商品id值
     * @return
     */
    public Long[] pushId(Integer len,Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i < ids.length; i++) {
            ids[i]=id;
        }
        return ids;
    }

}
