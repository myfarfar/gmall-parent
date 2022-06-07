import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;


public class Demo {


    @Autowired
    private static RestHighLevelClient restHighLevelClient;


    //todo:如何查询最大价格华为手机的额外字段
    public  void in()throws Exception {


        SearchRequest searchRequest = new SearchRequest("goods");
        //获得搜索构建器
        SearchSourceBuilder builder = searchRequest.source();
        //匹配查询 ，匹配title为华为手机
        //MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "华为手机");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", "Apple"));
        //boolQueryBuilder.filter(AggregationBuilders.max("ff").field())
//        builder.query(matchQueryBuilder);
        //获取最大价格
        AggregationBuilder aggregationBuilder= AggregationBuilders.max("price").field("price");
        builder.aggregation(aggregationBuilder);
        builder.fetchSource(new String[]{"id","defaultImg","title","price","createTime"},null);


        //客户端执行搜索
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        search.getHits().forEach(hit->{
            Aggregations aggregations = search.getAggregations();
            //获取聚合Aggregation信息
            Map<String, Aggregation> stringAggregationMap = aggregations.asMap();

        });

    }
}
