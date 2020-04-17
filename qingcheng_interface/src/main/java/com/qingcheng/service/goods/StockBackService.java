package com.qingcheng.service.goods;

import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

public interface StockBackService {

    /**
     * 在数据库添加一条回滚数据
     * @param orderItemList
     */
    public void addList(List<OrderItem> orderItemList);

    /**
     * 执行库存回滚
     */
    public void doBack();

}
