package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.StockBackMapper;
import com.qingcheng.pojo.goods.StockBack;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
@Service(interfaceClass = StockBackService.class)
public class StockBackServiceImpl implements StockBackService {


    @Autowired
    private StockBackMapper stockBackMapper;


    @Transactional
    public void addList(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            StockBack stockBack = new StockBack();
            stockBack.setSkuId(orderItem.getSkuId());
            stockBack.setOrderId(orderItem.getOrderId());
            stockBack.setNum(orderItem.getNum());
            stockBack.setStatus("0");//回滚状态 0 ：未回滚
            stockBack.setCreateTime(new Date());//创建时间
            stockBackMapper.insert(stockBack);//数据库添加一条回滚数据
        }
    }

    @Autowired
    private SkuMapper skuMapper;

    /**
     * 执行库存回滚
     */
    @Transactional
    public void doBack() {
        System.out.println("库存回滚任务开始");
        //查询库存回滚表中状态为0（未回滚）的记录
        StockBack stockBack0 = new StockBack();
        stockBack0.setStatus("0");
        List<StockBack> stockBackList = stockBackMapper.select(stockBack0);
        for (StockBack stockBack : stockBackList) {
            //1.添加库存
            skuMapper.deductionStock(-stockBack.getNum(),stockBack.getSkuId());
            //2.减少销量
            skuMapper.addSaleNum(-stockBack.getNum(),stockBack.getSkuId());
            stockBack.setStatus("1");//设置库存回滚对象的状态为已回滚 即为1
            stockBack.setBackTime(new Date());//设置回滚时间
            stockBackMapper.updateByPrimaryKey(stockBack);//更新数据库
        }
        System.out.println("库存回滚任务结束");

    }
}
