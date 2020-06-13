package com.leyou.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@Document(indexName = "goods", type = "docs", shards = 1, replicas = 0)
public class Goods {
    @Id
    private Long id; // spuId

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String all; // 被搜索的信息，包括title、category、brand
    @Field(type = FieldType.Keyword, index = false)
    private String subTitle;// 卖点
    private Long brandId;// 品牌id
    private Long cid1;// 三级分类
    private Long cid2;
    private Long cid3;
    private Date createTime;//spu创建时间
    private Set<Long> price;// 商品价格
    @Field(type = FieldType.Keyword,index = false)
    private String skus;// sku信息的json结构
    private Map<String,Object> specs;// 可搜索的规格参数，key是参数名，值是参数值

}
