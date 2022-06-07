package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper,BaseCategoryTrademark>
        implements BaseCategoryTrademarkService {


    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;


    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {
        //  根据分类Id 获取到品牌Id 集合数据
        //  select * from base_category_trademark where category3_id = ?;
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        //  判断baseCategoryTrademarkList 这个集合
        if(!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //  需要获取到这个集合中的品牌Id 集合数据
            List<Long> tradeMarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //  正常查询数据的话... 需要根据品牌Id 来获取集合数据！
            return baseTrademarkMapper.selectBatchIds(tradeMarkIdList);
        }
        //  如果集合为空，则默认返回空
        return null;
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (!CollectionUtils.isEmpty(trademarkIdList)){
            List<BaseCategoryTrademark> categoryTrademarkList = new ArrayList<>();
            for (Long aLong : trademarkIdList) {
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setTrademarkId(aLong);
                categoryTrademarkList.add(baseCategoryTrademark);
            }
            this.saveBatch(categoryTrademarkList);
        }

    }

    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<BaseCategoryTrademark>().eq("category3_id", category3Id);

        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            List<Long> trademarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> baseCategoryTrademark.getTrademarkId()).collect(Collectors.toList());
            return baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> !trademarkIdList.contains(baseTrademark.getId())).collect(Collectors.toList());
        }
        return baseTrademarkMapper.selectList(null);


    }
    @Override
    public void remove(Long category3Id, Long trademarkId) {

        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper();
        queryWrapper.eq("category3_id",category3Id);
        queryWrapper.eq("trademark_id",trademarkId);
        baseCategoryTrademarkMapper.delete(queryWrapper);

    }
}
