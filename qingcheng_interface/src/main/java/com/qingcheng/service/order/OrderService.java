package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderDetail;
import com.qingcheng.pojo.order.Orders;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);


    public Order findById(String id);

    public Map<String,Object> add(Order order);


    public void update(Order order);


    public void delete(String id);

    public OrderDetail getOrderDetailById(String orderId);


    List<Order> queryOrderStatus(String[] ids);

    void batchSend(List<Order> orderList);

    public void orderTimeOutLogic();

    public void mergeOrder(String orderId1,String orderId2);

    /**
     * 修改订单状态
     * @param orderId
     * @param transactionId 交易流水号
     */
    public void updatePayStatus(String orderId,String transactionId);

    /**
     * 通过orderId关闭订单
     * @param orderId 订单Id
     */
    public void closeOrder(String orderId);




}
