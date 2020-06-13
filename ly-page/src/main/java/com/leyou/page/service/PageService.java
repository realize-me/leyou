package com.leyou.page.service;

import com.leyou.item.pojo.*;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PageService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private SpecificationClient specClient;

    public Map<String, Object> loadModel(Long spuId) {
        Map<String, Object> model = new HashMap<>();

        Spu spu = goodsClient.querySpuById(spuId);
        SpuDetail spuDetail = spu.getSpuDetail();
        List<Sku> skus = spu.getSkus();

        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        List<SpecGroup> specGroups = specClient.queryGroupsByCid(spu.getCid3());

        // 规格参数的id和name
        HashMap<Long, String> paramMap = new HashMap<>();
        for (SpecGroup specGroup : specGroups) {
            List<SpecParam> params = specGroup.getParams();
            for (SpecParam param : params) {
                paramMap.put(param.getId(),param.getName());
            }
        }


        model.put("spu", spu);
        model.put("skus", skus);
        model.put("detail", spuDetail);
        model.put("brand", brand);
        model.put("categories", categories);
        model.put("specs", specGroups);
        model.put("paramMap", paramMap);

        return model;
    }

    public void createHtml(Long spuId){
        //上下文
        Context context = new Context();
        context.setVariables(loadModel(spuId));
        File dest = new File("D:\\html", spuId + ".html");
        if(dest.exists()){
            dest.delete();
        }
        try (PrintWriter writer = new PrintWriter(dest, "UTF-8")) {

            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            log.error("[静态页服务] 生成静态页异常!",e);
        }

    }

    public void deleteHtml(Long spuId) {
        File dest = new File("D:\\html", spuId + ".html");
//        File dest = new File("/opt/nginx/html/item/", spuId + ".html");
        if(dest.exists()){
            dest.delete();
        }
    }
}
