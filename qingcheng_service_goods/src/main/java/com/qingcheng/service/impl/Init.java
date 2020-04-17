package com.qingcheng.service.impl;

import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpecService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Init implements InitializingBean {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuService skuService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpecService specService;

    //工程启动之后要进行的逻辑
    public void afterPropertiesSet() throws Exception {
        //缓存预热
        System.out.println("缓存预热");
        System.out.println("加载商品分类导航");
        categoryService.saveCategoryTreeToRedis();//加载商品分类导航
        System.out.println("加载价格数据");
        skuService.saveAllPriceToRedis();//加载价格数据
        System.out.println("加载品牌数据");
        brandService.saveAllBrandToRedis();//加载品牌进缓存
        System.out.println("加载规格数据");
        specService.saveAllSpecToRedis();//加载规格进缓存

    }
}
