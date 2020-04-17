package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.goods.User;
import com.qingcheng.service.goods.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Reference
    private UserService userService;

    @GetMapping("/findAll")
    public List<User> findAll(){
        return userService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<User> findPage(int page, int size){
        return userService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<User> findList(@RequestBody Map<String,Object> searchMap){
        return userService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<User> findPage(@RequestBody Map<String,Object> searchMap,int page, int size){
        return  userService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public User findById(Long id){
        return userService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody User user){
        userService.add(user);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody User user){
        userService.update(user);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(Long id){
        userService.delete(id);
        return new Result();
    }

}
