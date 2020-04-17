package com.qingcheng.consumer;

import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class OrderMessageConsumer implements MessageListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private WeixinPayService weixinPayService;


    @Override
    public void onMessage(Message message) {
        //1.提取消息
        String orderId = new String(message.getBody());
        //2.查询订单状态
        Order order = orderService.findById(orderId);
        if ("0".equals(order.getPayStatus())){//业务系统订单为未支付
            //调用微信查询订单方法
            Map map = weixinPayService.queryPaymentResult(orderId);
            if ("SUCCESS".equals(map.get("trade_state"))){
                //微信已支付 实现补偿 即设置业务订单系统支付状态为 已支付
                order.setPayStatus("1");
            }else {
                //微信未支付 关闭微信支付订单 并设置业务订单为关闭
                weixinPayService.closeOrder(orderId);
                order.setOrderStatus("4");
            }
        }
    }
}
