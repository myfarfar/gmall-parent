package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 首页数据
     * @param model
     * @return
     */
    @RequestMapping({"/","index.html"})
    public String getBaseCategoryList(Model model){
        // 通过skuId 查询skuInfo
        Result result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list",result.getData());
        return "index/index";
    }


}
