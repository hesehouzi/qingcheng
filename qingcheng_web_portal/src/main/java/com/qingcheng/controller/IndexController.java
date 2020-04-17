package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Reference
    private AdService adService;

    @Reference
    private CategoryService categoryService;

    @GetMapping("/index")
    public String index(Model model){
        //查询首页轮播图
        System.out.println("查询首页轮播图");
        List<Ad> lbList = adService.findByPosition("web_index_lb");
        model.addAttribute("lbt",lbList);

        //商品的分类导航
        System.out.println("查询分类导航");
        List<Map> categoryTree = categoryService.findCategoryTree();
        model.addAttribute("catrgoryList",categoryTree);
        return "index";
    }

}
