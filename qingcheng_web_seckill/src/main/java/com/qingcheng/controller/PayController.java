package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
import com.qingcheng.service.seckill.SeckillOrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private SeckillOrderService seckillOrderService;

    @Reference
    private WeixinPayService weixinPayService;


    /**
     * 创建微信支付二维码
     * @return
     */
    @RequestMapping("/createNativeSeckillOrder")
    public Map createNative(){
        //获取用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //调用业务逻辑 查询对应的秒杀订单
        SeckillOrder seckillOrder = seckillOrderService.findByUsername(username);
        if (null!=seckillOrder){
            System.out.println(seckillOrder.getId().toString());
            //调用微信支付接口
            Map map = weixinPayService.createNative(seckillOrder.getId().toString(), seckillOrder.getMoney().intValue(), "http://localhost:9104/pay/notify.do");
            return map;
        }
        return null;
    }

    /**
     * 根据orderId查询秒杀订单支付状态
     * @param orderId 秒杀订单id
     * @return
     */
    @RequestMapping("/queryPayStatus")
    public Map queryPayStatus(String orderId){
        //获得用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //支付状态查询
        Map<String,String> map = weixinPayService.queryPaymentResult(orderId);
        //判断支付状态
        if (map.get("return_code").equals("SUCCESS")
            && map.get("result_code").equals("SUCCESS")
            && map.get("trade_state").equals("SUCCESS")){//三个状态码同时为SUCCESS 表示支付成功
            //成功则修改订单状态
            System.out.println(map.get("time_end"));
            seckillOrderService.updateOrderStatus(orderId,username,map.get("transaction_id"),map.get("time_end"));
        }
        return map;
    }

    /**
     * 回调方法
     * @param request
     */
    @RequestMapping("/notify")
    public void notifyLogic(HttpServletRequest request){
        System.out.println("支付成功回调............................................");
        //1.先得到一个输入流
        try {
            ServletInputStream inputStream = request.getInputStream();
            //创建一个输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //创建一个字节数组 大小是1024个字节
            byte[] buffer = new byte[1024];
            int len=0;
            //进行读取 读取之后输出
            while ((len=inputStream.read(buffer))!=-1){
                outputStream.write(buffer,0,len);
            };
            //关闭流
            outputStream.close();
            inputStream.close();
            String result = new String(outputStream.toByteArray(),"utf-8");
            System.out.println("回调结果："+result);
            weixinPayService.notifyLogic(result);//调用验证方法

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
