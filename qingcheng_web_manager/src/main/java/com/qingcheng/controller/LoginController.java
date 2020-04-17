package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Reference
    private LoginLogService loginLogService;

    @GetMapping("/getName")
    public Map getName(){
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        HashMap map = new HashMap();
        map.put("name",name);
        return map;
    }

    @GetMapping("/findPageByLogin")
    public PageResult<LoginLog> findPageByLogin(int page,int size){
        //添加查询条件
        //1.通过安全框架获得登录人名字
        String LoginName = SecurityContextHolder.getContext().getAuthentication().getName();
        //2.构建map集合
        HashMap map = new HashMap();
        map.put("loginName",LoginName);
        return loginLogService.findPage(map,page,size);
    }

}
