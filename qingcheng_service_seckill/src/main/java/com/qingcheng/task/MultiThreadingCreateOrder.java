package com.qingcheng.task;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.util.IdWorker;
import com.qingcheng.util.SeckillStatus;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送延时消息
     * @param seckillStatus
     */
    public void sendDelayMessage(SeckillStatus seckillStatus){
        rabbitTemplate.convertAndSend(
                "exchange.delay.order.begin",
                "delay",
                JSON.toJSONString(seckillStatus),
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setExpiration("10000");//设置信息过期时间
                        return message;
                    }
                }
        );



    }

    /**
     * 创建秒杀订单
     */
    @Async
    public void createOrder(){
        try {
            SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
            if (null!=seckillStatus){
                Object goodsId = redisTemplate.boundListOps("SeckillGoodsList_" + seckillStatus.getGoodsId()).rightPop();
                if (goodsId==null){
                    //清空用户下单信息
                    clearUserQueue(seckillStatus);
                    return;
                }
                System.out.println("准备下单...");
                Thread.sleep(10000);
                System.out.println("开始下单...");
                //从Redis中获取username、商品id、秒杀时间段time
                String username = seckillStatus.getUsername();
                Long id = seckillStatus.getGoodsId();
                String time = seckillStatus.getTime();
                //1.获得商品数据 从缓存中查询
                SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);

                //BigDecimal num = new BigDecimal(1);
                //3.如果有库存 创建秒杀订单
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setId(idWorker.nextId());//设置秒杀订单id
                seckillOrder.setSeckillId(id);//设置秒杀商品id
                seckillOrder.setMoney(seckillGoods.getCostPrice());//设置秒杀商品秒杀金额 秒杀单价*数量
                seckillOrder.setUserId(username);//设置用户id
                seckillOrder.setSellerId(seckillGoods.getSellerId());//设置商家id
                seckillOrder.setCreateTime(new Date());//设置创建时间
                seckillOrder.setStatus("0");//设置订单状态 未支付
                //4.将秒杀订单存入Redis中
                redisTemplate.boundHashOps("SeckillOrder").put(username,seckillOrder);
                //5.库存减少
                Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), -1);
                seckillGoods.setStockCount(surplusCount.intValue());
                //5.1判断当前商品是否还有库存
                if (surplusCount==0){
                    //5.1.1没有库存 并且将该商品数据同步到MySql中
                    seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
                    //5.1.2 清空Redis中该商品
                    redisTemplate.boundHashOps("SeckillGoods_"+time).delete(id);
                }else {//如果有库存 则直接将数据重置到Redis中
                    redisTemplate.boundHashOps("SeckillGoods_"+time).put(id,seckillGoods);

                }
                //更新抢单状态
                seckillStatus.setStatus(2);//待支付
                seckillStatus.setOrderId(seckillOrder.getId());//订单id
                seckillStatus.setMoney(seckillOrder.getMoney().floatValue());//订单价格
                //更新订单的状态信息
                redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
                System.out.println("下单成功...");
                //发送延迟消息
                sendDelayMessage(seckillStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清理用户排队信息
     * @param seckillStatus
     */
    public void clearUserQueue(SeckillStatus seckillStatus){
        //1.排队信息清空
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());
        //2.清空递增排队标识
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());

    }

}
