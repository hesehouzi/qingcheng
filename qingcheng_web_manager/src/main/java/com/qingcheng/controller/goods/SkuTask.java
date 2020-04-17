package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SkuTask {

    @Reference
    private StockBackService stockBackService;

    /**
     * 间隔一小时执行一次库存回滚
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void orderTimeOutLogic(){
        System.out.println("执行库存回滚");
        stockBackService.doBack();
    }

}
