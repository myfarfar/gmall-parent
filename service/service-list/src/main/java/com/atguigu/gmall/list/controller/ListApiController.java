package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;


    @Autowired
    private SearchService searchService;
    /**
     * 创建索引库
     * @return
     */
    @GetMapping("inner/createIndex")
    public Result createIndex() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }



    //  上架
    @ApiOperation("上架")
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    //  下架
    @ApiOperation("下架")
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }



    //  热度
    @ApiOperation("热度")
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        // 调用服务层
        searchService.incrHotScore(skuId);
        return Result.ok();

    }

    /**
     * 搜索商品
     * @param searchParam
     * @return
     * @throws IOException
     */
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo response = searchService.search(searchParam);
        return Result.ok(response);
    }


}
