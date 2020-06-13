package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bouncycastle.jcajce.provider.symmetric.Serpent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsRepository repository;
    @Autowired
    private ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu) {
        // 查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categories)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> names = categories.stream().map(Category::getName).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        // 搜索字段
        String all = spu.getTitle() + StringUtils.join(names, " ") + brand.getName();
        //查询skus
        List<Sku> skuList = goodsClient.querySkuBySpuId(spu.getId());
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        // 价格集合
        Set<Long> priceList = new HashSet<>();
        // 对skuList处理
        ArrayList<Map<String, Object>> skus = new ArrayList<>();
        for (Sku sku : skuList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            skus.add(map);
            // 处理价格
            priceList.add(sku.getPrice());

        }
        // 查询规格参数
        List<SpecParam> params = specificationClient.queryParamByList(null, spu.getCid3(), true);
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.queryDetailById(spu.getId());
        // 通用规格参数
        Map<Long, String> genericSpec = JsonUtils.parseMap(spuDetail.getGenericSpec(), Long.class, String.class);
        // 特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });

        // 规格参数的key 和value
        HashMap<String, Object> specs = new HashMap<>();
        for (SpecParam param : params) {
            String key = param.getName();
            Object value = "";
            if(param.getGeneric()){
                value = genericSpec.get(param.getId());
                // 数值类型
                if (param.getNumeric()) {
                    // 处理成段
                    value = chooseSegment(value.toString(), param);
                }
            }else{
                value = specialSpec.get(param.getId());
            }
            specs.put(key, value);
        }

        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setSubTitle(spu.getSubTitle());
        goods.setCreateTime(spu.getCreateTime());

        goods.setAll(all); // 搜索字段，包括标题、分类、品牌、规格等
        goods.setPrice(priceList); // 所有sku价格的集合
        goods.setSkus(JsonUtils.serialize(skus)); // 所有sku集合的json格式
        goods.setSpecs(specs); //  所有可搜素的规格参数



        return goods;
    }

    private String chooseSegment(String value,SpecParam specParam){
        double val = NumberUtils.toDouble(value);
        String result = "其他";
        for (String segment : specParam.getSegments().split(",")) {
            String[] segs = segment.split("-");
            //获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val>=begin && val<end){
                if (segs.length == 1) {
                    result = segs[0] + specParam.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + specParam.getUnit() + "以下";
                }else{
                    result = segment + specParam.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest request) {
        int page = request.getPage() - 1;
        int size = request.getSize();
        // 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 字段过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 分页
        queryBuilder.withPageable(PageRequest.of(page, size));
        // 过滤
//        MatchQueryBuilder baseQuery = QueryBuilders.matchQuery("all", request.getKey());
        QueryBuilder baseQuery = buildBasicQuery(request);
        queryBuilder.withQuery(baseQuery);
        // 聚合分类和品牌
        // 聚合分类
        String categoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        // 聚合品牌
        String brandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 解析分页结果
        int totalPages = result.getTotalPages();
        long totalElements = result.getTotalElements();
        List<Goods> goodsList = result.getContent();
        // 解析聚合结果
        Aggregations aggs = result.getAggregations();
        List<Category> categoryList = parseCategory(aggs.get(categoryAggName));
        List<Brand> brandList = parseBrand(aggs.get(brandAggName));
        // 聚合规格参数
        List<Map<String, Object>> specList = null;
        if (categoryList != null && categoryList.size() == 1) {
            specList = buildSpecificationAggs(categoryList.get(0).getId(), baseQuery);
        }
        return new SearchResult(totalElements, totalPages, goodsList, categoryList, brandList, specList);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 创建布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 添加查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 添加过滤项
        Map<String, String> filterMap = request.getFilter();
        Set<Map.Entry<String, String>> entries = filterMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            // 过滤项为不为分类和品牌的 进行处理
            if(!"cid3".equals(key) && !"brandId".equals(key) ){
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }
        return boolQueryBuilder;
    }

    private List<Map<String, Object>> buildSpecificationAggs(Long id, QueryBuilder baseQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 查询分类下的规格参数
        List<SpecParam> specParams = specificationClient.queryParamByList(null, id, true);
        // 根据规格参数聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(baseQuery);
        for (SpecParam specParam : specParams) {
            String specName = specParam.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(specName).field("specs." + specName + ".keyword"));
        }
        // 获取聚合结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 解析聚合结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam specParam : specParams) {
            String specName = specParam.getName();
            StringTerms terms = aggs.get(specName);
            List<String> options = terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());
            Map<String, Object> map = new HashMap<>();
            map.put("k", specName);
            map.put("options", options);
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> parseBrand(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Brand> brandList = brandClient.queryBrandByIds(ids);
            return brandList;
        }catch (Exception e){
            log.error("[搜索服务]：查询品牌异常");
            return null;
        }
    }

    private List<Category> parseCategory(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Category> categories = categoryClient.queryCategoryByIds(ids);
            return categories;
        }catch (Exception e){
            log.error("[搜索服务]：查询分类异常");
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * 监听到item-service微服务新增或修改商品后，更新索引库
     * 如果抛出异常，spring自带ACK机制，因此消息不会丢失
     * @param spuId
     */
    public void createOrUpdateGoods(Long spuId) {
        Spu spu = goodsClient.querySpuById(spuId);
        Goods goods = buildGoods(spu);
        repository.save(goods);
    }

    /**
     * 监听到item-servic微服务删除商品后，将索引库中对应的数据删除
     * @param spuId
     */
    public void deleteIndex(Long spuId) {
        repository.deleteById(spuId);
    }
}
