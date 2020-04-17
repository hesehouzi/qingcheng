package com.qingcheng.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.pay.WeixinPayService;
import com.qingcheng.util.SeckillStatus;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息监听类
 */
@Component(value = "orderMessageListener")
public class OrderMessageListener implements MessageListener {

    @Autowired
    private RedisTemplate redisTemplate;

    @Reference
    private WeixinPayService weixinPayService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 秒杀库存回滚
     * @param seckillStatus
     */
    public void rollBackOrder(SeckillStatus seckillStatus){
        //查询Redise中是否存在对应的秒杀订单
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(seckillStatus.getUsername());
        //存在，即已经超时，关闭订单，并回滚库存
        if (seckillOrder!=null){
            //1.关闭微信支付订单
            Map<String, String> map = weixinPayService.closeOrder(seckillStatus.getOrderId().toString());
            //1.判断取消结果 如果取消成功
            if (map==null || "SUCCESS".equals(map.get("return_code"))&&"SUCCESS".equals(map.get("result_code"))){
                //2.本地取消订单 取消存在redis中的订单
                redisTemplate.boundHashOps("SeckillOrder").get(seckillStatus.getUsername());
                //3.回滚库存(Redis中存在库存或者不存在库存)
                SeckillGoods seckillGoods= (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + seckillStatus.getTime()).get(seckillStatus.getGoodsId());
                if (null==seckillGoods){
                    seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillGoods.getGoodsId());
                }
                //内存增加
                seckillGoods.setStockCount(seckillGoods.getStockCount()+1);
                //Redis中数量标记增加1
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(),1);
                //同步到Redis中
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).put(seckillStatus.getGoodsId(),seckillGoods);
                //同步数据到队列中
                redisTemplate.boundListOps("SeckillGoodsList_"+seckillStatus.getGoodsId()).leftPush(seckillStatus.getGoodsId());

                //4.清理用户排队抢单信息
                //1.排队信息清空
                redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());
                //2.清空递增排队标识
                redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());

            }
            //取消订单失败 出现错误
        }

    }



    /**
     * 消息监听方法
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        //获取消息
        String content = new String(message.getBody());
        //System.out.println("接收到的消息是："+content);
        //将jsonString数据转换成对应的SeckillStatus对象
        SeckillStatus seckillStatus = JSON.parseObject(content, SeckillStatus.class);
        //调用库存回滚
        rollBackOrder(seckillStatus);
    }

}
