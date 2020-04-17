package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.goods.Auditlog;
import com.qingcheng.service.goods.AuditlogService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auditlog")
public class AuditlogController {

    @Reference
    private AuditlogService auditlogService;

    @GetMapping("/findAll")
    public List<Auditlog> findAll(){
        return auditlogService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Auditlog> findPage(int page, int size){
        return auditlogService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Auditlog> findList(@RequestBody Map<String,Object> searchMap){
        return auditlogService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Auditlog> findPage(@RequestBody Map<String,Object> searchMap,int page, int size){
        return  auditlogService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public Auditlog findById(Integer id){
        return auditlogService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Auditlog auditlog){
        auditlogService.add(auditlog);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Auditlog auditlog){
        auditlogService.update(auditlog);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(Integer id){
        auditlogService.delete(id);
        return new Result();
    }

}
