package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
/**  1. ES原生 客户端: TransportClient(即将废除)    RestClient
 *   2. spring整合的 Spring Data Elasticsearch
 *      ==>ElasticsearchTemplate是TransportClient客户端  (不怎么用)
 *      ==> ElasticsearchRestTemplate是RestHighLevel客户端 => 用于创建索引,映射,文档查询
 *   3. Repository
 *
 * 所以常用两种客户端: ElasticsearchRestTemplate  和 Repository
 *
 * ElasticsearchRestTemplate：基于High level rest client
 * 			createIndex(User.class)
 * 			putMapping(User.class)
 * ElasticsearchRepository：CRUD 分页排序
 * 	        save/saveAll
 * 			deleteById(1l)
 * 			findById()
 */

@SpringBootTest
class GmallSearchApplicationTests {
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;

    /**
     * 由于数据导入只需导入一次，
     * 这里就写一个测试用例。后续索引库和数据库的数据同步，通过程序自身来维护。
     */

    @Test
    void contextLoads() {
//        // 创建索引
//        elasticsearchRestTemplate.createIndex(Goods.class);
//        // 创建映射
//        elasticsearchRestTemplate.putMapping(Goods.class);

        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            //查询条件
            PageParamVo pageParamVo = new PageParamVo(pageNum, pageSize, null);
            //查询spu
            ResponseVo<List<SpuEntity>> spuByPageJson = this.pmsClient.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities = spuByPageJson.getData();
            //非空验证
            if (!CollectionUtils.isEmpty(spuEntities)) {
                return;
            }
            //根据spuid查询spu下的每个sku
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skusBySpuId = this.pmsClient.querySkusBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skusBySpuId.getData();
                //非空验证
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        goods.setSkuId(skuEntity.getId());
                        goods.setImage(skuEntity.getDefaultImage());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setCreateTime(spuEntity.getCreateTime());

                        //根据品牌id查询并赋值品牌
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        //根据分类id查询并赋值分类
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        //根据skuid查询并赋值销量 库存
                        ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {

                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                            // 获取里面的销量 进行相加
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b)->(a+b)).get().intValue());
                        }

                        //创建setSearchAttrs集合接收
                        List<SearchAttrValue> searchAttrValueList = new ArrayList<>();
                        //根据skuid查询并赋值spu和sku的规格参数 ==> 添加到集合
                        ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueBySpuId = this.pmsClient.querySearchAttrValueBySpuId(spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = querySearchAttrValueBySpuId.getData();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                            searchAttrValueList.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValue);

                                return searchAttrValue;
                            }).collect(Collectors.toList()));
                        }

                        ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuId = this.pmsClient.querySearchAttrValueBySkuId(skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = querySearchAttrValueBySkuId.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                            searchAttrValueList.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);

                                return searchAttrValue;
                            }).collect(Collectors.toList()));
                        }

                        goods.setSearchAttrs(searchAttrValueList);
                        return goods;
                    }).collect(Collectors.toList());
                    this.goodsRepository.saveAll(goodsList);
                }
            });

            pageSize = spuEntities.size();
            pageNum++;
        }while (pageSize == 100);
    }

}
