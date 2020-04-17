package com.qingcheng.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/redirect")
public class RedirectController {

    /**
     * 通用跳转
     * @param referer
     * @return
     */
    @RequestMapping("/back")
    public String backMethod(@RequestHeader(value = "referer",required = false)String referer){
        if (!StringUtils.isEmpty(referer)){
            return "redirect:"+referer;
        }
        return "/seckill-index.html";
    }

}
