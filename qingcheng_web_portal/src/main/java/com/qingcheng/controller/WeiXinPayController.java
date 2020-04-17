package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
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
@RequestMapping("/wxpay")
public class WeiXinPayController {

    @Reference
    private OrderService orderService;

    @Reference
    private WeixinPayService weixinPayService;


    @GetMapping("/createNative")
    public Map createNative(String orderId){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderService.findById(orderId);
        if (order!=null){//订单存在
            //进行校验 付款状态 订单状态 username
            if ("0".equals(order.getPayStatus()) && "0".equals(order.getOrderStatus()) && username.equals(order.getUsername())){
                //校验成功 调用付款
                Map map = weixinPayService.createNative(orderId, order.getPayMoney(), "http://hesehouzi.easy.echosite.cn/wxpay/notify.do");
                return map;
            }else {//校验不成功
                return null;
            }

        }else {//订单不存在
            return null;
        }
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
