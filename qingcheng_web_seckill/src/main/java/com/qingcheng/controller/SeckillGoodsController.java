package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import com.qingcheng.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/seckill/goods")
public class SeckillGoodsController {

    @Reference
    private SeckillGoodsService seckillGoodsService;


    /**
     * 获取时间菜单
     * @return
     */
    @RequestMapping("/menus")
    public List<Date> dateMenus(){
        return DateUtil.getDateMenus();
    }

    /**
     * 获取对应时间的秒杀商品
     * @param date
     * @return
     */
    @RequestMapping("/list")
    public List<SeckillGoods> getList(String date) {
        System.out.println(date);
        return seckillGoodsService.findListByTimeSlot(DateUtil.formatStr(date));
    }

    /**
     * 根据id和秒杀开始时间查询秒杀对象
     * @param time
     * @param id
     * @return
     */
    @RequestMapping("/one")
    public SeckillGoods getOne(String time,Long id){
        return seckillGoodsService.getOne(time,id);
    }

}
