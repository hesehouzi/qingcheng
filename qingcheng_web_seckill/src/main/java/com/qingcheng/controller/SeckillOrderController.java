package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.util.SeckillStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill/order")
public class SeckillOrderController {

    @Reference
    private SeckillOrderService seckillOrderService;

    /**
     * 通过id 和 时间 添加秒杀订单 匿名访问：anonymousUser
     * @param id
     * @param time
     * @return
     */
    @RequestMapping("/addOrder")
    public Result addOrder(Long id, String time){
        try {
            //通过springSecurity获得username
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(username)){//403表示用户未登录
                return new Result(403,"未登录！");
            }
            Boolean boo = seckillOrderService.addOrder(id, time, username);
            if (boo){
                return new Result(0,"恭喜你，下单成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(1,e.getMessage());
        }
        return new Result(1,"很遗憾，下单失败");
    }

    /**
     * 通过用户名查询秒杀订单排队状态
     * @return
     */
    @RequestMapping("/query")
    public Result queryStatus(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(username)){
            return new Result(403,"用户未登录");
        }
        SeckillStatus seckillStatus = seckillOrderService.queryStatus(username);
        if (null!=seckillStatus){
            Result result = new Result(seckillStatus.getStatus(), "这是抢单状态");
            result.setOther(seckillStatus);
            return result;
        }
        return new Result(404,"该用户没有任排队信息！！");
    }

}
