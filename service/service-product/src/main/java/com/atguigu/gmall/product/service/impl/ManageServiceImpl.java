package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuPosterMapper spuPosterMapper;


    @Autowired
    private SkuInfoMapper skuInfoMapper;


    @Autowired
    private SkuImageMapper skuImageMapper;


    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;


    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;



    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return   baseTrademarkMapper.selectById(tmId);
    }

    //    ??????????????????????????????  ?????????
    @Override
    @GmallCache(prefix = "getBaseCategoryList:")
    public List<JSONObject> getBaseCategoryList() {
//        ??????json??????
        ArrayList<JSONObject> list = new ArrayList<>();
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
//        ????????????????????????????????????json
        Map<Long, List<BaseCategoryView>> category1Map =
                baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        // ?????????????????????????????????
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            Long category1Id = entry1.getKey();
            //?????????????????????????????????
            List<BaseCategoryView> category2List1 = entry1.getValue();
            JSONObject category1 = new JSONObject();
//            ?????????json?????????
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            //??????????????????
            category1.put("categoryName", category2List1.get(0).getCategory1Name());

            index++;
            //        ??????????????????????????????
            Map<Long, List<BaseCategoryView>> category2Map =
                    category2List1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            // ??????????????????????????????
            List<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // ??????????????????Id
                Long category2Id = entry2.getKey();
                List<BaseCategoryView> category3List = entry2.getValue();
                // ????????????????????????
                JSONObject category2 = new JSONObject();

                //            ?????????json?????????
                category2.put("categoryId", category2Id);
                //??????????????????
                category2.put("categoryName", category3List.get(0).getCategory2Name());
                category2Child.add(category2);

                List<JSONObject> category3Child = new ArrayList<>();
                category3List.stream().forEach(category3View -> {
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", category3View.getCategory3Id());
                    category3.put("categoryName", category3View.getCategory3Name());
                    category3Child.add(category3);
                });
//                ????????????????????????
                category2.put("categoryChild", category3Child);
            }
            //  ????????????????????????


            category1.put("categoryChild", category2Child);
            list.add(category1);


        }

        return list;

    }


    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            // ?????????????????????key-value
            // ??????key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // ???????????????????????? redis ????????????????????? ?????????????????????????????? ???????????????????????????
            // ??????????????????
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // ???????????????????????????????????????
            if (skuInfo == null) {
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ????????????redisson
                // ????????????key sku:skuId:lock  set k1 v1 px 10000 nx
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
            /*
            ???????????? lock.lock();
            ?????????:  lock.lock(10,TimeUnit.SECONDS);
            ???????????? lock.tryLock(100,10,TimeUnit.SECONDS);
             */
                // ????????????
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res) {
                    try {
                        // ?????????????????? ???????????????????????????
                        // ????????????????????????????????? {??????????????????????????????????????? = ??????????????????}
                        skuInfo = getSkuInfoDB(skuId);
                        // ???????????????????????????????????????
                        if (skuInfo == null) {
                            // ???????????????????????? ?????????????????????????????????
                            SkuInfo skuInfo1 = new SkuInfo(); //???????????????
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        // ?????????????????????????????????
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

                        // ??????redis ?????????lua ???????????? ???????????????????????? lock.unlock
                        return skuInfo;

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // ?????????
                        lock.unlock();
                    }
                } else {
                    // ??????????????????
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            } else {

                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // ??????????????????????????????????????????????????????
        return getSkuInfoDB(skuId);
    }


    // ??????redis' ???????????????
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            // ?????????????????????key-value
            // ??????key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // ???????????????????????? redis ????????????????????? ?????????????????????????????? ???????????????????????????
            // ??????????????????
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // ???????????????????????????????????????
            if (skuInfo == null) {
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ????????????redis ???????????????redisson
                // ????????????key sku:skuId:lock  set k1 v1 px 10000 nx
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                // ???????????????
                String uuid = UUID.randomUUID().toString().replace("-", "");
                // ??????
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (isExist) {
                    // ?????????????????????????????????
                    System.out.println("????????????????????????");
                    // ????????????????????????????????? {??????????????????????????????????????? = ??????????????????}
                    skuInfo = getSkuInfoDB(skuId);
                    // ???????????????????????????????????????
                    if (skuInfo == null) {
                        // ???????????????????????? ?????????????????????????????????
                        SkuInfo skuInfo1 = new SkuInfo(); //???????????????
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    // ?????????????????????????????????
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    // ???????????????lua ????????????
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    // ??????lua???????????????????????????
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    // ??????lua?????????????????????Long
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    // ??????key ???????????? value
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);

                    return skuInfo;
                } else {
                    // ??????????????????
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            } else {

                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // ??????????????????????????????????????????????????????
        return getSkuInfoDB(skuId);
    }


    @Override
    public SkuInfo getSkuInfoDB(Long skuId) {

        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
            skuInfo.setSkuImageList(skuImageList);
        }

        return skuInfo;
    }


    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {

        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }


    @Override
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        QueryWrapper queryWrapper = new QueryWrapper<SpuPoster>();
        queryWrapper.eq("spu_id", spuId);
        List<SpuPoster> list = spuPosterMapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {

        Map map = new HashMap();

        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            for (Map map1 : mapList) {
                map.put(map1.get("value_ids"), map1.get("sku_id"));
            }
        }
        return map;
    }

    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            return skuInfo.getPrice();
        }
        return new BigDecimal(0);
    }

    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        System.out.println("------------------------");
      /*  SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<SkuImage>();
        queryWrapper.eq("sku_id", skuId);
        List<SkuImage> skuImages = skuImageMapper.selectList(queryWrapper);
        skuInfo.setSkuImageList(skuImages);
        return skuInfo;*/

        // ????????????redisson?????????????????????
//        return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
        // return getSkuInfoRedis(skuId);

    }


    @Override
    public IPage<SkuInfo> getPage(Page<SkuInfo> pageParam) {
        QueryWrapper<SkuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");

        IPage<SkuInfo> page = skuInfoMapper.selectPage(pageParam, queryWrapper);

        return page;
    }

    @Override
    @Transactional
    public void onSale(Long skuId) {
        // ??????????????????
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(1);
        skuInfoMapper.updateById(skuInfoUp);

        //????????????
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);

    }

    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        // ??????????????????
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(0);
        skuInfoMapper.updateById(skuInfoUp);


        //????????????
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();

        if (skuImageList != null && skuImageList.size() > 0) {

            // ????????????
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        // ????????????????????????
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }


    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id", spuId);
        return spuImageMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuInfoMapper.insert(spuInfo);


        //????????????sput????????????
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }

        }


        //??????????????????????????????
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

//                ??????????????????????????????
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttrValue.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }

                }
            }
        }

//        ??????posterList????????????
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            for (SpuPoster spuPoster : spuPosterList) {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            }

        }
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper queryWrapper = new QueryWrapper<BaseCategory2>();
        queryWrapper.eq("category1_id", category1Id);
        List<BaseCategory2> list = baseCategory2Mapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {

        QueryWrapper queryWrapper = new QueryWrapper<BaseCategory3>();
        queryWrapper.eq("category2_id", category2Id);
        return baseCategory3Mapper.selectList(queryWrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //???????????????????????????
        if (baseAttrInfo.getId() != null) {
            baseAttrInfoMapper.updateById(baseAttrInfo);
        } else {
            baseAttrInfoMapper.insert(baseAttrInfo);
        }


        //BaseAttrValue???????????????  ????????????????????????attr_id ???baseAttrInfo.getId()????????????
        QueryWrapper queryWrapper = new QueryWrapper<BaseAttrValue>();
        queryWrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(queryWrapper);

        //??????????????????????????????????????????
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //??????????????????id???baseAttrValue
                baseAttrValue.setAttrId(baseAttrInfo.getCategoryId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }

    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //??????????????????????????????????????????????????????
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }


    //??????ID???????????????
    private List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper queryWrapper = new QueryWrapper<BaseAttrValue>();
        queryWrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(queryWrapper);

    }


    //spu????????????
    @Override
    public IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> page, SpuInfo spuInfo) {

        QueryWrapper queryWrapper = new QueryWrapper<SpuInfo>();
        queryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        queryWrapper.orderByAsc("id");

        return spuInfoMapper.selectPage(page, queryWrapper);
    }


}
