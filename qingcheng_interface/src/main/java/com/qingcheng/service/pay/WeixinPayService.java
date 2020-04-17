package com.qingcheng.service.pay;

import java.util.Map;

/**
 * 微信支付接口
 */
public interface WeixinPayService {

    /**
     * 生成微信支付二维码
     * @param orderId 订单号
     * @param money 金额（分）
     * @param notifyUrl 回调地址
     * @return
     */
    public Map createNative(String orderId,Integer money,String notifyUrl);

    /**
     * 微信支付回调
     * @param xml
     */
    public void notifyLogic(String xml);

    /**
     * 根据订单号查询支付结果
     * @param orderId
     * @return
     */
    public Map queryPaymentResult(String orderId);

    /**
     * 根据订单号关闭订单
     * @param orderId
     */
    public Map<String,String> closeOrder(String orderId);

}
