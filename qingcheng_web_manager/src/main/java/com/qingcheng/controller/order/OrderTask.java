package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.order.CategoryReportService;
import com.qingcheng.service.order.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class OrderTask {

    @Reference
    private OrderService orderService;

    @Reference
    private CategoryReportService categoryReportService;

    @Scheduled(cron = "0 0/2 * * * ?")
    public void orderTimeOutLogic(){
        System.out.println("每两分钟执行一次任务"+new Date());
        orderService.orderTimeOutLogic();//订单超时处理
    }

    /**
     * 定时任务 每天凌晨1点统计昨天的数据，插入到tb_category_report中
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void createCategoryReportDate(){
        System.out.println("createCategoryReportDate...");
        categoryReportService.creataData();//数据统计
    }

}
