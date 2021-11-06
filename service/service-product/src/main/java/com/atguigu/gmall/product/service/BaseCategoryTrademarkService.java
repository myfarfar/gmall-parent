package com.atguigu.gmall.product.service;


import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {

    /**
     * 根据三级分类ID获取品牌
     */

    List<BaseTrademark> findTrademarkList(Long  category3Id);


    /**
     * 保存分类和品牌关联
     */

   void save(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 获取当前未被三级分类关联的所有品牌
     */

    List<BaseTrademark> findCurrentTrademarkList(Long  category3Id);

    /**
     * 删除分类和品牌关联
     */

    void remove(Long  category3Id,Long trademarkId );
}
