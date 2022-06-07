package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Api(tags = "商品管理")
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("根据spuId 获取spuImage 集合")
    @GetMapping("spuImageList/{spuId}")
    public Result<List<SpuImage>> getSpuImageList(@PathVariable Long spuId){
        //  需要调用服务层方法
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    //保存spu
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){

         manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }




    //获取所有销售属性
    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){

        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }




    /**
     * 分页查询Spu
     *
     * @return
     */
    @GetMapping("{page}/{size}")
    public Result getSpuInfoPage(@PathVariable Long page  ,@PathVariable Long size,SpuInfo spuInfo) {
        //创建一个page对象
        Page<SpuInfo> spuInfoPage = new Page<>(page,size);
//        获取数据
        IPage<SpuInfo> page1 = manageService.getSpuInfoPage(spuInfoPage, spuInfo);

        return Result.ok(page1);

    }

}
