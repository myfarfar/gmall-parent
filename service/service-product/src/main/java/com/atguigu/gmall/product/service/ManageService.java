package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {

    /**
     * 通过品牌Id 来查询数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);



    /**
     * 获取全部分类信息
     *
     */

    List<JSONObject> getBaseCategoryList();


    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */

     SkuInfo getSkuInfoDB(Long skuId);

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);



    /**
     * 根据spuid获取商品海报
     * @param spuId
     * @return
     */
    List<SpuPoster> findSpuPosterBySpuId(Long spuId);


    /**
     * 根据spuId 查询map 集合属性
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);



    /**
     * 根据spuId，skuId 查询销售属性集合
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);


    /**
     * 获取sku价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);


    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);


/**
     * 根据skuId 查询skuInfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);



    /**
     * SKU分页列表
     * @param pageParam
     * @return
     */
    IPage<SkuInfo> getPage(Page<SkuInfo> pageParam);
    /**
     * 商品上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void cancelSale(Long skuId);



    /**
     * 保存数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);


    /**
     * 根据spuId 查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);


    /**
     * 根据spuId 查询spuImageList
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);


    /**
     * 保存商品 数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);


    /**
     * 查询所有销售属性
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();


    /**
     * 查询所有一级分类信息
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 查询所有二级分类信息
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);
    /**
     * 查询所有三级分类信息
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);


    /**
     * 根据分类Id 获取平台属性数据
     *  * 接口说明：
     *  *      1，平台属性可以挂在一级分类、二级分类和三级分类
     *  *      2，查询一级分类下面的平台属性，传：category1Id，0，0；   取出该分类的平台属性
     *  *      3，查询二级分类下面的平台属性，传：category1Id，category2Id，0；
     *  *         取出对应一级分类下面的平台属性与二级分类对应的平台属性
     *  *      4，查询三级分类下面的平台属性，传：category1Id，category2Id，category3Id；
     *  *         取出对应一级分类、二级分类与三级分类对应的平台属性
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */

    List<BaseAttrInfo> getAttrInfoList(Long category1Id,Long category2Id,Long category3Id);

    /**
     * 保存平台属性方法
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);
    /**
     * 查询平台属性方法
     * @param attrId
     */
    BaseAttrInfo  getAttrInfo(Long attrId);


    IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> page, SpuInfo spuInfo);


}
