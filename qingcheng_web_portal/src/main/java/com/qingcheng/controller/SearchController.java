package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SearchController {

    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("/search")
    private String search(Model model, @RequestParam Map<String,String> searchMap) throws Exception {
        //字符集处理
        //searchMap=WebUtil.convertCharsetToUTF8(searchMap);
        //页面容错 如果没有传递页码 设置 pageNo为1
        if (searchMap.get("pageNo")==null){
            searchMap.put("pageNo","1");

        }
        //容错处理页面传递给后端 两个参数 sort：排序字段 sortOrder：排序规则
        if (searchMap.get("sort")==null){
            searchMap.put("sort","");
        }
        if (searchMap.get("sortOrder")==null){
            searchMap.put("sortOrder","DESC");
        }


        //远程调用接口
        Map result = skuSearchService.search(searchMap);//调用search方法
        model.addAttribute("result",result);
        //url处理
        StringBuffer url = new StringBuffer("/search.do?");
        for (String key : searchMap.keySet()) {
            url.append("&"+key+"="+searchMap.get(key));
        }
        model.addAttribute("url",url);
        model.addAttribute("searchMap",searchMap);

        //页码处理
        int pageNo=Integer.parseInt(searchMap.get("pageNo"));//当前页码
        model.addAttribute("pageNo",pageNo);

        Long totalPages= (Long) result.get("totalPages");//总页数
        int startPage=1;//开始页码
        int endPage=totalPages.intValue();//结束页码
        if (totalPages>5){
            startPage=pageNo-2;
            if (startPage<1){
                startPage=1;
            }
            endPage=startPage+4;
        }


        model.addAttribute("totalPages",totalPages);//总页数
        model.addAttribute("startPage",startPage);//开始页码
        model.addAttribute("endPage",endPage);//结束页码

        return "search";
    }

}
