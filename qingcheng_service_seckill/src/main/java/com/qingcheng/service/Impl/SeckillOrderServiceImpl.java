package com.qingcheng.service.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.task.MultiThreadingCreateOrder;
import com.qingcheng.util.DateUtil;
import com.qingcheng.util.SeckillStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import java.text.DateFormat;
import java.util.Date;

@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {


    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;




    /**
     * 添加秒杀订单
     * @param id 商品id
     * @param time 活动时间
     * @param username 当前用户
     * @return
     */
    @Override
    public Boolean addOrder(Long id, String time, String username) {

        //判断当前商品库存个数是否为0
        Long size = redisTemplate.boundListOps("SeckillGoodsList_" + id).size();
        //如果size<=0 或者为null 抛出异常 101 表示商品已售罄
        if (size==null || size<0){
            throw new RuntimeException("101");
        }


        //创建一个递增数据
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        if (userQueueCount>1){
            //抛出异常 状态码100 表示重复排队
            throw new RuntimeException("100");
        }

        //创建用户排队信息
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(), 1, id, time);
        //用户抢单信息存入队列（下单使用）这里采用list存入 list本身是一个队列
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);
        //将抢单状态存入Redis中
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
        //异步调用 多线程方法
        multiThreadingCreateOrder.createOrder();
        return true;
    }

    /**
     * 根据用户名查询订单状态
     * @param username
     * @return
     */
    @Override
    public SeckillStatus queryStatus(String username) {
        return (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username );
    }

    /**
     * 更新商品订单状态
     * @param outtradeno 商品订单id
     * @param username 用户名
     * @param transactionid 交易流水号
     */
    @Override
    public void updateOrderStatus(String outtradeno, String username, String transactionid,String time_end) {
        //查询用户订单信息
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        //将从缓存中查询到的订单数据同步到MySql中
        seckillOrder.setStatus("1");//已支付
        seckillOrder.setTransactionId(transactionid);//设置交易流水号

        seckillOrder.setPayTime(DateUtil.formatDate(time_end));//设置支付创建时间
        seckillOrderMapper.insertSelective(seckillOrder);//数据库添加数据
        //清理Redis中秒杀订单信息
        redisTemplate.boundHashOps("SeckillOrder").delete(username);
        //清理Redis中的相关信息
        //清理用户排队信息和递增排队信息
        clearUserQueue(username);
    }

    /**
     * 根据用户名查询秒杀订单  从缓存中查询
     * @param username
     * @return
     */
    @Override
    public SeckillOrder findByUsername(String username) {
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        return seckillOrder;
    }

    /**
     * 根据用户 清理相关信息
     * @param username
     */
    public void clearUserQueue(String username){
        //1.排队信息清空
        redisTemplate.boundHashOps("UserQueueCount").delete(username);
        //2.清空递增排队标识
        redisTemplate.boundHashOps("UserQueueStatus").delete(username);
    }

}
