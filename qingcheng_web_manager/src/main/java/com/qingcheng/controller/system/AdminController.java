package com.qingcheng.controller.system;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.AdminAndRoles;
import com.qingcheng.service.system.AdminService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;


import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Reference
    private AdminService adminService;

    @GetMapping("/findAll")
    public List<Admin> findAll(){
        return adminService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Admin> findPage(int page, int size){
        return adminService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Admin> findList(@RequestBody Map<String,Object> searchMap){
        return adminService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Admin> findPage(@RequestBody Map<String,Object> searchMap,int page, int size){
        return  adminService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public AdminAndRoles findById(Integer id){
        return adminService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody AdminAndRoles adminAndRoles){
        adminService.add(adminAndRoles);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody AdminAndRoles adminAndRoles){
        adminService.update(adminAndRoles);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(Integer id){
        adminService.delete(id);
        return new Result();
    }

    @GetMapping("/updatePassword")
    public Result updatePassword(String oldPassword,String newPassword){
        //1.获得用户登录名
        String loginName = SecurityContextHolder.getContext().getAuthentication().getName();
        //2.查询加密密码
        Admin admin = adminService.findAdminByUsername(loginName);
        //2.1进行非空判断
        if (admin==null){
            return new Result(1,"对不起，无此用户！");
        }
        //3.密码校验
        boolean checkpw = BCrypt.checkpw(oldPassword, admin.getPassword());
        if (checkpw){//如果旧密码正确 即checkpw为true
            admin.setPassword(BCrypt.hashpw(newPassword,BCrypt.gensalt()));
            AdminAndRoles adminAndRoles = new AdminAndRoles();
            adminAndRoles.setAdmin(admin);
            adminService.update(adminAndRoles);
            return new Result();
        }else {
            return new Result(1,"密码不正确！！");
        }

    }

}
