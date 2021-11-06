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

        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<BaseCategoryTrademark>().eq("category3_id",category3Id);
        List<BaseCategoryTrademark> list = baseCategoryTrademarkMapper.selectList(queryWrapper);
       if (!CollectionUtils.isEmpty(list)){

           list.stream().map(baseCategoryTrademark ->baseCategoryTrademark.getBaseTrademark()).collect(Collectors.toList());
       }

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
