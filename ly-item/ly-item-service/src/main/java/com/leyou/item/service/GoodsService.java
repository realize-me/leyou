package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.FlowAdapters;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.Transient;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        // 分页
        PageHelper.startPage(page,rows);
        // 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title", "%"+key+"%");
        }
        if(saleable!=null){
            criteria.andEqualTo("saleable", saleable);
        }
        // 排序
        example.setOrderByClause("last_update_time DESC");
        // 查询
        List<Spu> spus = spuMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(spus)){
           throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        // 解析分类和品牌名称
        loadBrandAndCategoryName(spus);

        // 解析分页结果
        PageInfo<Spu> info = new PageInfo<>(spus);

        return new PageResult<>(info.getTotal(),spus);
    }

    private void loadBrandAndCategoryName(List<Spu> spus) {
        for (Spu spu : spus) {
            // 解析分类
            List<String> names = categoryService.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names, "/"));
            // 解析品牌
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());
        }
    }

    public void saveGoods(Spu spu) {
        // 新增spu
        spu.setId(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(true);
        spu.setValid(false);
        int count = spuMapper.insert(spu);
        if(count!=1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        // 新增detail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        detailMapper.insert(spuDetail);

        saveSkuAndStock(spu);

        // 发送mq消息
        amqpTemplate.convertAndSend("item.insert", spu.getId());

    }

    void saveSkuAndStock(Spu spu){
        // 新增库存
        List<Sku> skus = spu.getSkus();
        List<Stock> stocks = new ArrayList<Stock>();
        int count=0;
        for (Sku sku : skus) {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());

            count = skuMapper.insert(sku);
            if(count!=1){
                throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
            }
            Stock stock = new Stock();
            stock.setStock(sku.getStock());
            stock.setSkuId(sku.getId());
            stocks.add(stock);
        }
        // 批量新增库存
        count = stockMapper.insertList(stocks);
        if(count!=stocks.size()){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    public SpuDetail queryDetailById(Long spuId) {

        SpuDetail spuDetail = detailMapper.selectByPrimaryKey(spuId);
        if(spuDetail==null){
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return spuDetail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        // 查询stock
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
//        List<Stock> stockList = stockMapper.selectByIdList(ids);
//        if(CollectionUtils.isEmpty(stockList)){
//            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
//        }
//        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
//        skuList.forEach(s->s.setStock(stockMap.get(s.getId())));
        loadStockInSku(ids, skuList);
        return skuList;
    }

    private void loadStockInSku(List<Long> ids, List<Sku> skuList) {
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(stockList)){
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s->s.setStock(stockMap.get(s.getId())));
    }


    @Transactional
    public void updateGoods(Spu spu) {
        if (spu.getId() == null) {
            throw new LyException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }

        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());

        List<Sku> skuList = skuMapper.select(sku);
        if(!CollectionUtils.isEmpty(skuList)){
            // 删除sku
            skuMapper.delete(sku);
            // 删除stock
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        // 修改spu
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);

        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if(count!=1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        // 修改detail
        count = detailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if(count!=1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        // 新增sku和库存
        saveSkuAndStock(spu);

        // 发送mq消息
        amqpTemplate.convertAndSend("item.update", spu.getId());
    }

    public void outSpu(Long spuId) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        if(spu==null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        if(spu.getSaleable()){
            spu.setSaleable(false);
        }else{
            spu.setSaleable(true);
        }
        spuMapper.updateByPrimaryKey(spu);
//        Example example = new Example(Spu.class);
//        Example.Criteria criteria = example.createCriteria();
//        criteria.andEqualTo("id", spuId);
//
//        int count = spuMapper.updateByExampleSelective(spu, example);
//        if(count!=1){
//            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
//        }
    }
    @Transactional
    public void deleteGoods(Long spuId) {
//         查询所有sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        List<Long> ids = new ArrayList<Long>();
        for (Sku sku1 : skuList) {
            ids.add(sku1.getId());
        }

        // 删除stock
        int count = stockMapper.deleteByIdList(ids);
//        if(count!=skuList.size()){
//            throw new LyException(ExceptionEnum.GOODS_DELETE_ERROR);
//        }
        // 删除spu下的所有sku
        count = skuMapper.deleteByIdList(ids);
//        if(count!=skuList.size()){
//            throw new LyException(ExceptionEnum.GOODS_DELETE_ERROR);
//        }
        // 删除spu
        count = spuMapper.deleteByPrimaryKey(spuId);
        if(count!=1){
            throw new LyException(ExceptionEnum.GOODS_DELETE_ERROR);
        }

    }

    public Spu querySpuById(Long id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        spu.setSkus(querySkuBySpuId(id));
        spu.setSpuDetail(queryDetailById(id));
        return spu;
    }

    public List<Sku> querySkuByIds(List<Long> ids) {
        List<Sku> skuList = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        loadStockInSku(ids, skuList);
        return skuList;
    }

    public void decreaseStock(List<CartDTO> cartDTOS) {
        for (CartDTO cartDTO : cartDTOS) {
            int count = stockMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if (count != 1) {
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
