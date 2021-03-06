package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.list.Repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {


    @Autowired
    private ProductFeignClient productFeignClient;


    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;



    @Override
    public void upperGoods(Long skuId) {

        //  ??????productFeignClient ?????????????????????Goods??????
        Goods goods = new Goods();

        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            //  Sku????????????
            goods.setId(skuInfo.getId());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());
            //  ??????skuInfo
            return skuInfo;
        });

        //  Sku????????????
        CompletableFuture<Void> categoryCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory1Name(categoryView.getCategory1Name());
        });


        //  Sku???????????????
        CompletableFuture<Void> tmCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        });


        //   Sku?????????????????????
        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            //  ?????????????????????
            List<SearchAttr> searchAttrList = attrList.stream().map((baseAttrInfo) -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                //  ????????????
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //  ??????????????????
                String valueName = baseAttrInfo.getAttrValueList().get(0).getValueName();
                searchAttr.setAttrValue(valueName);

                return searchAttr;
            }).collect(Collectors.toList());

            goods.setAttrs(searchAttrList);
        });

        //  ?????????????????????
        CompletableFuture.allOf(skuInfoCompletableFuture,
                categoryCompletableFuture,
                tmCompletableFuture,
                attrCompletableFuture).join();


        //        Goods goods = new Goods();
        //        //  Sku????????????
        //        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //        goods.setId(skuInfo.getId());
        //        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        //        goods.setPrice(skuInfo.getPrice().doubleValue());
        //        goods.setTitle(skuInfo.getSkuName());
        //        goods.setCreateTime(new Date());
        //        //  Sku????????????
        //        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        //
        //        goods.setCategory1Id(categoryView.getCategory1Id());
        //        goods.setCategory2Id(categoryView.getCategory2Id());
        //        goods.setCategory3Id(categoryView.getCategory3Id());
        //        goods.setCategory3Name(categoryView.getCategory3Name());
        //        goods.setCategory2Name(categoryView.getCategory2Name());
        //        goods.setCategory1Name(categoryView.getCategory1Name());
        //
        //
        //        //  Sku???????????????
        //        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        //        goods.setTmId(trademark.getId());
        //        goods.setTmName(trademark.getTmName());
        //        goods.setTmLogoUrl(trademark.getLogoUrl());
        //
        //        //  Sku?????????????????????
        //        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        //
        //        //  ????????????
        //        List<SearchAttr> searchAttrList = new ArrayList<>();
        //        //  ????????????
        //        for (BaseAttrInfo baseAttrInfo : attrList) {
        //            SearchAttr searchAttr = new SearchAttr();
        //            searchAttr.setAttrId(baseAttrInfo.getId());
        //            //  ????????????
        //            searchAttr.setAttrName(baseAttrInfo.getAttrName());
        //            //  ??????????????????
        //            String valueName = baseAttrInfo.getAttrValueList().get(0).getValueName();
        //            searchAttr.setAttrValue(valueName);
        //        }
        //        goods.setAttrs(searchAttrList);

        //  ???????????????es?????????
        this.goodsRepository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        //  ??????
        this.goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        // ??????key
        String hotKey = "hotScore";
        // ????????????
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
            if (hotScore % 10 == 0) {
            // ??????es
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);

        }


    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        // ??????dsl??????
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(response);

        SearchResponseVo responseVO = this.parseSearchResult(response);
        responseVO.setPageSize(searchParam.getPageSize());
        responseVO.setPageNo(searchParam.getPageNo());
        long totalPages = (responseVO.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();
        responseVO.setTotalPages(totalPages);
        return responseVO;
    }
    // ?????????????????????
    private SearchResponseVo parseSearchResult(SearchResponse response) {
        SearchHits hits = response.getHits();
        // ????????????
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        // ?????????????????????
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        // ParsedLongTerms ?
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo trademark = new SearchResponseTmVo();
            // ????????????Id
            trademark.setTmId((Long.parseLong(((Terms.Bucket) bucket).getKeyAsString())));
            //            trademark.setTmId(Long.parseLong(bucket.getKeyAsString()));
            // ??????????????????
            Map<String, Aggregation> tmIdSubMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdSubMap.get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();

            trademark.setTmName(tmName);

            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdSubMap.get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            trademark.setTmLogoUrl(tmLogoUrl);

            return trademark;
        }).collect(Collectors.toList());
        searchResponseVo.setTrademarkList(trademarkList);

        // ??????????????????
        SearchHit[] subHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        if (subHits!=null && subHits.length>0){
            // ????????????
            for (SearchHit subHit : subHits) {
                // ???subHit ???????????????
                Goods goods = JSONObject.parseObject(subHit.getSourceAsString(), Goods.class);

                // ????????????
                if (subHit.getHighlightFields().get("title")!=null){
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);

        // ????????????????????????
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)){
            List<SearchResponseAttrVo> searchResponseAttrVOS = buckets.stream().map(bucket -> {
                // ????????????????????????
                SearchResponseAttrVo responseAttrVO = new SearchResponseAttrVo();
                // ?????????????????????Id
                responseAttrVO.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");




                List<? extends Terms.Bucket> nameBuckets = attrNameAgg.getBuckets();
                responseAttrVO.setAttrName(nameBuckets.get(0).getKeyAsString());
                // ????????????????????????
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                List<String> values = valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                responseAttrVO.setAttrValueList(values);
                return responseAttrVO;
            }).collect(Collectors.toList());
            searchResponseVo.setAttrsList(searchResponseAttrVOS);
        }
        // ??????????????????
        searchResponseVo.setTotal(hits.getTotalHits().value);

        return searchResponseVo;
    }


    // ??????dsl ??????
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        // ???????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // ??????boolQueryBuilder
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // ?????????????????????????????? ?????????
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            // ????????????  ??????and??????
            // MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("title",searchParam.getKeyword()).operator(Operator.AND);
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }
        // ??????????????????
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)){
            // trademark=2:??????
            String[] split = StringUtils.split(trademark, ":");
            if (split != null && split.length == 2) {
                // ????????????Id??????
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", split[0]));
            }
        }

        // ?????????????????? ?????????????????????????????????????????????????????????????????????term
        if(null!=searchParam.getCategory1Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }
        // ??????????????????
        if(null!=searchParam.getCategory2Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        // ??????????????????
        if(null!=searchParam.getCategory3Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }

        // ????????????????????????
        // 23:4G:????????????
        String[] props = searchParam.getProps();
        if (props!=null &&props.length>0){
            // ????????????
            for (String prop : props) {
                // 23:4G:????????????
                String[] split = StringUtils.split(prop, ":");
                if (split!=null && split.length==3){
                    // ??????????????????
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // ?????????????????????
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    // ????????????==?????????????????????
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    // ScoreMode.None ???
                    boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));
                    // ??????????????????????????????
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        // ??????????????????
        searchSourceBuilder.query(boolQueryBuilder);
        // ????????????
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        // ??????
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)){
            // ??????????????????
            String[] split = StringUtils.split(order, ":");
            if (split!=null && split.length==2){
                // ???????????????
                String field = null;
                // ???????????????????????????
                switch (split[0]){
                    case "1":
                        field="hotScore";
                        break;
                    case "2":
                        field="price";
                        break;
                }
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                // ?????????????????????????????????
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }

        // ????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.postTags("</span>");
        highlightBuilder.preTags("<span style=color:red>");

        searchSourceBuilder.highlighter(highlightBuilder);

        //  ??????????????????
        TermsAggregationBuilder termsAggregationBuilder =        AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));

        searchSourceBuilder.aggregation(termsAggregationBuilder);

        //  ????????????????????????
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));


        // ???????????????
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);

        SearchRequest searchRequest = new SearchRequest("goods");
        //searchRequest.types("_doc");
        searchRequest.source(searchSourceBuilder);
        System.out.println("dsl:"+searchSourceBuilder.toString());
        return searchRequest;
    }


}
