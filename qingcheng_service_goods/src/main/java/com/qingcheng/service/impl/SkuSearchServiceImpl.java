package com.qingcheng.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.pojo.goods.Brand;
import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.service.goods.SpecService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class SkuSearchServiceImpl implements SkuSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpecMapper specMapper;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpecService specService;

    /**
     * 布尔组合查询  查询name中包含keywords的并且分类名称为选定的category的
     * @param searchMap
     * @return
     */
    public Map search(Map<String, String> searchMap) {
        //1.封装查询请求
        SearchRequest searchRequest = new SearchRequest("sku");//指定要查询的索引库
        //设置查询的类型 类比与MySQL中的table 表名
        searchRequest.types("doc");//指定查询的类型
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();//查询源构建器 相当于kibana语法中的【query】
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();//布尔查询构建器
        //1.1关键字搜索-name中包含keywords的
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", searchMap.get("keywords"));
        boolQueryBuilder.must(matchQueryBuilder);
        //1.2商品分类过滤-指定选定的category
        if (null!=searchMap.get("category")){
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("categoryName", searchMap.get("category"));//指定category的值为词条
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //1.3品牌分类过滤
        if (null!=searchMap.get("brand")){
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", searchMap.get("brand"));//
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //1.4规格过滤
        for (String key : searchMap.keySet()) {
            if (key.startsWith("spec.")){//如果规格是规格参数 这里跟前端进行约定
                TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key + ".keyword", searchMap.get(key));
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //1.5价格过滤
        if (null!=searchMap.get("price")){
            String[] price = searchMap.get("price").split("-");
            if (!price[0].equals("0")){//最低价格不等于0
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(price[0] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
            if (!price[1].equals("*")){//最高价格有上限
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").lte(price[1] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
        }

        //聚合查询 商品分类
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("sku_category").field("categoryName");//构建词条聚合构建器
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        searchSourceBuilder.query(boolQueryBuilder);
        //分页
        Integer pageNo = Integer.parseInt(searchMap.get("pageNo"));//页码
        Integer pageSize=30;//每页显示条数
        int fromIndex = (pageNo - 1) * pageSize;//起始索引

        searchSourceBuilder.from(fromIndex);//开始索引设置
        searchSourceBuilder.size(pageSize);//每页记录数
        //排序
        String sort =searchMap.get("sort");//排序字段
        String sortOrder = searchMap.get("sortOrder");//排序规则
        if (!"".equals(sort)){//若排序字段为!"" 即排序
            searchSourceBuilder.sort(sort, SortOrder.valueOf(sortOrder));//执行排序操作
        }
        //高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name").preTags("<font style='color:red'>").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(searchSourceBuilder);

        //2.封装查询结果
        Map resultMap = new HashMap();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits searchHits = searchResponse.getHits();
            long totalHits = searchHits.getTotalHits();//获得所有记录数
            System.out.println("记录数："+totalHits);
            SearchHit[] hits = searchHits.getHits();//获得所有分片对象

            //2.1商品列表
            List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
            for (SearchHit hit : hits) {
                Map<String, Object> skuMap = hit.getSourceAsMap();
                //name高亮处理
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField name = highlightFields.get("name");
                Text[] fragments = name.fragments();
                skuMap.put("name",fragments[0].toString());//将高亮部分的name替换原来的name
                resultList.add(skuMap);
            }
            resultMap.put("rows",resultList);

            //2.2商品分类列表
            Aggregations aggregations = searchResponse.getAggregations();
            Map<String, Aggregation> aggregationMap = aggregations.getAsMap();

            Terms terms = (Terms) aggregationMap.get("sku_category");//获得他的子类型terms
            List<? extends Terms.Bucket> buckets = terms.getBuckets();
            List<String> categoryList =new ArrayList<String>();
            for (Terms.Bucket bucket : buckets) {
                categoryList.add(bucket.getKeyAsString());
            }
            System.out.println(categoryList);
            resultMap.put("categoryList",categoryList);

            //2.3品牌列表
            String categoryName = "";//商品分类名称
            if (searchMap.get("category") == null) {
                if (categoryList.size() > 0) {
                    categoryName = categoryList.get(0);//提取分类列表中的第一个分类
                    System.out.println(categoryName);
                }
            } else {
                categoryName = searchMap.get("category");
            }
            if (null==searchMap.get("brand")) {
                //List<Map> brandList = brandMapper.findListByCategoryName(categoryName);//直接通过数据库交互查询到品牌列表
                List<Map> brandList = brandService.findBrandFromRedis(categoryName);//从缓存中查询
                resultMap.put("brandList", brandList);
            }
            //2.4规格列表
            //List<Map> specList = specMapper.findListByCategoryName(categoryName);//从数据库查询得到规格列表
            List<Map> specList = specService.findSpecFromRedisByCategoryName(categoryName);
            for (Map spec : specList) {
                String[] options = ((String) spec.get("options")).split(",");//获得string类型的数组
                System.out.println(options);
                spec.put("options",options);
            }
            resultMap.put("specList",specList);

            //2.5 页码渲染 totalCount：总记录数； pageSize：每页显示记录数； pageNo：页码 ；pageCount：总页数
            long totalCount = searchHits.getTotalHits();//总记录数
            long pageCount =(totalCount%pageSize==0)?totalCount/pageSize:(totalCount/pageSize+1);//总页数
            resultMap.put("totalPages",pageCount);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultMap;
    }
}
