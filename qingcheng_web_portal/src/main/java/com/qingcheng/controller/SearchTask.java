package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.SpecService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SearchTask {

    @Reference
    private BrandService brandService;

    @Reference
    private SpecService specService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void saveAllBrandToRedis(){
        System.out.println("开启定时任务：保存数据库品牌数据到缓存....");
        brandService.saveAllBrandToRedis();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void saveAllSpecToRedis(){
        System.out.println("开启定时任务：保存数据库规格数据到缓存....");
        specService.saveAllSpecToRedis();
    }

}
