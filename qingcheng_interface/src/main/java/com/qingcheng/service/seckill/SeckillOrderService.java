package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.util.SeckillStatus;

public interface SeckillOrderService {

    /**
     * 秒杀商品下单
     * @param id 商品id
     * @param time 活动时间
     * @param username 当前用户
     * @return
     */
    public Boolean addOrder(Long id,String time,String username);

    /**
     * 根据用户名查询秒杀订单状态
     * @param username
     * @return
     */
    public SeckillStatus queryStatus(String username);

    /**
     * 更新秒杀订单状态
     * @param outtradeno 商品订单id
     * @param username 用户名
     * @param transactionid 交易流水号
     * @param time_end   支付时间
     */
    public void updateOrderStatus(String outtradeno,String username,String transactionid,String time_end);

    /**
     * 根据用户名查询秒杀订单
     * @param username
     * @return
     */
    public SeckillOrder findByUsername(String username);

}
