package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author atguigu-mqx
 */
@Api(tags = "品牌管理")
@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //  http://localhost/admin/product/baseTrademark/1/10
    @ApiOperation("分页列表")
    @GetMapping("{page}/{limit}")
    public Result getPage(@PathVariable Long page,
                          @PathVariable Long limit){

        //  创建一个Page 对象
        Page<BaseTrademark> baseTrademarkPage = new Page<BaseTrademark>(page,limit);

        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.getPage(baseTrademarkPage);
        //  返回数据
        return Result.ok(baseTrademarkIPage);
    }

    //  添加品牌数据：
    //  http://api.gmall.com/admin/product/baseTrademark/save
    //  前端传递的是Json 数据 ,需要将其转换为JavaObject
    @ApiOperation("保存")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        //  保存方法
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //  删除数据
    //  http://localhost/admin/product/baseTrademark/remove/11
    @ApiOperation("删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        //  调用服务层方法
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //  回显数据
    //  http://api.gmall.com/admin/product/baseTrademark/get/{id}
    @ApiOperation("获取详情")
    @GetMapping("get/{id}")
    public Result<BaseTrademark> getData(@PathVariable Long id){
        //  调用服务层方法
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    //  修改的按钮
    //  http://api.gmall.com/admin/product/baseTrademark/update
    //  通过api 数据接口文档，得知前端传递的是Json 数据
    @ApiOperation("修改")
    @PutMapping("update")
    public Result updateData(@RequestBody BaseTrademark baseTrademark){
        //  调用服务层方法
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    //  http://api.gmall.com/admin/product/baseTrademark/getTrademarkList
    @ApiOperation("获取全部品牌")
    @GetMapping("getTrademarkList")
    public Result<List<BaseTrademark>> getTrademarkList(){
        //  select * from base_trademark
        List<BaseTrademark> list = baseTrademarkService.list(null);
        //  返回
        return Result.ok(list);
    }


/*    //  根据品牌的关键字检索品牌列表
    @ApiOperation("根据品牌的关键字检索品牌列表")
    @GetMapping("findBaseTrademarkByKeyword/{keyword}")
    public Result<List<BaseTrademark>> findBaseTrademarkByKeyword(@PathVariable String keyword){
        //  调用服务层方法
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.findBaseTrademarkByKeyword(keyword);
        return Result.ok(baseTrademarkList);
    }

    // 根据品牌Id 集合获取对应的品牌集合数据
    @ApiOperation("根据品牌Id 集合获取对应的品牌集合数据")
    @PostMapping("inner/findBaseTrademarkByTrademarkIdList")
    public List<BaseTrademark> findBaseTrademarkByTrademarkIdList(@RequestBody List<Long> tmIdList){
        return baseTrademarkService.findBaseTrademarkByTrademarkIdList(tmIdList);
    }*/
}
