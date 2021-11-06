package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;



     /**
     * 保存分类品牌
     *
     * @return
     */

    @PostMapping("save")
    public Result save(@RequestBody BaseCategoryTrademark baseCategoryTrademark) {
        //创建一个BaseTrademark对象
        baseCategoryTrademarkService.save(baseCategoryTrademark);

        return Result.ok();

    }
    /**
     * 删除分类品牌
     *
     * @return
     */
    @DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result remove(@PathVariable Long category3Id, @PathVariable Long trademarkId) {
        baseCategoryTrademarkService.remove(category3Id,trademarkId);

        return Result.ok();

    }



    /**
     * 根据三级ID获取品牌
     *
     * @return
     */
    @GetMapping("findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long  category3Id  ) {
        List<BaseTrademark> trademarkList = baseCategoryTrademarkService.findTrademarkList(category3Id);

        return Result.ok(trademarkList);

    }
    /**
     * 获取未绑定的分类品牌
     *
     * @return
     */
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long  category3Id  ) {
        List<BaseTrademark> trademarkList = baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);

        return Result.ok(trademarkList);

    }
}
