package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


/**
 * 异步编排CompletableFuture
 *  		初始化方法：
 * 			supplyAsync：任务有返回结果集
 * 				supplyAsync(() -> {
 *                                })
 * 			runAsync：没有返回结果集
 * 				runAsync(() -> {
 *                })
 *
 * 		计算完成方法：
 * 			whenComplete()
 * 			whenCompleteAsync()
 * 				上述方法：上一个任务正常执行或者出现异常时都可以执行该子任务
 * 			exceptionnally()
 * 				上一个任务出现异常时会执行该子任务
 *
 * 		串行化方法：
 * 			thenApply：可以获取上一个任务的返回结果，并给下一个任务返回自己的结果
 * 			thenAccept：可以获取上一个任务的返回结果，但是不会给下一个任务返回自己的结果
 * 			thenRun：上一个任务执行完成即执行该任务，既不获取上一个任务的返回结果，也不给下一个任务返回自己的结果
 * 			都有异步方法，并且有异步同载方法。
 *
 * 		组合方法：
 * 			allOf：所有任务都完成才执行新任务
 * 			anyOf：任何一个任务完成执行新任务
 *
 */
@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        CompletableFuture<SkuEntity> skuEntityCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 1.根据skuId查询sku，设置sku相关信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            if (skuEntityResponseVo == null) {
                return null;
            }
            SkuEntity skuEntity = skuEntityResponseVo.getData();
                itemVo.setSkuId(skuEntity.getId());
                itemVo.setTitle(skuEntity.getTitle());
                itemVo.setSubTitle(skuEntity.getSubtitle());
                itemVo.setPrice(skuEntity.getPrice());
                itemVo.setWeight(skuEntity.getWeight());
                itemVo.setDefaultImage(skuEntity.getDefaultImage());

            return skuEntity;
        },threadPoolExecutor);

        CompletableFuture<Void> cateCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 2.根据sku中的categoryId查询一二三级分类
            ResponseVo<List<CategoryEntity>> categoriesByCid = this.pmsClient.query123categoriesByCid(skuEntity.getCatagoryId());
            List<CategoryEntity> categoryEntities = categoriesByCid.getData();
            if (!CollectionUtils.isEmpty(categoryEntities)) {
                itemVo.setCategories(categoryEntities);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> brandCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 3.根据sku中的brandId查询品牌
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> spuCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 4.根据sku中的spuId查询spu
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> skuImagesCompletableFuture = CompletableFuture.runAsync(() -> {
            // 5.根据skuId查询sku的图片信息
            ResponseVo<List<SkuImagesEntity>> imagesByskuId = this.pmsClient.queryImagesByskuId(skuId);
            List<SkuImagesEntity> imagesEntityList = imagesByskuId.getData();
            if (!CollectionUtils.isEmpty(imagesEntityList)) {
                itemVo.setImages(imagesEntityList);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> itemCompletableFuture = CompletableFuture.runAsync(() -> {
            // 6.根据skuId查询sku的营销信息
            ResponseVo<List<ItemSaleVo>> itemSaleVo = this.smsClient.querysalesByskuId(skuId);
            List<ItemSaleVo> itemSaleVoList = itemSaleVo.getData();
            if (!CollectionUtils.isEmpty(itemSaleVoList)) {
                itemVo.setSales(itemSaleVoList);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
            // 7.根据skuId查询库存信息
            ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> saleAttrCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 8.根据sku中的spuId查询spu下所有sku的选择值集合
            ResponseVo<List<SaleAttrValueVo>> saleAttrValueBySpuId = this.pmsClient.queryAllSaleAttrValueBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrValueBySpuId.getData();
            if (!CollectionUtils.isEmpty(saleAttrValueVos)) {
                itemVo.setSaleAttrs(saleAttrValueVos);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> skuAttrCompletableFuture = CompletableFuture.runAsync(() -> {
            // 9.根据skuId查询当前sku的销售属性 (需要转为map [AttrId:AttrValue])
            ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValueBySkuId = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = querySaleAttrValueBySkuId.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                Map<Long, String> collect = skuAttrValueEntities.stream().collect(Collectors.toMap(map -> map.getAttrId(), map -> map.getAttrValue()));
                //两种方式
                //Map<Long, String> collect = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
                itemVo.setSaleAttr(collect);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> stringCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 10.根据sku中spuId查询spu下所有sku销售属性组合与skuId的映射关系
            ResponseVo<String> stringResponseVo = this.pmsClient.querySaleAttrMappingSkuIdBySpuId(skuEntity.getSpuId());
            String skuJson = stringResponseVo.getData();
            if (skuJson != null) {
                itemVo.setSkuJson(skuJson);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> spuDescCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 11.根据sku中spuId查询spu的描述信息（海报信息） (需要以逗号分开 转集合)
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null && StringUtils.isNotBlank(spuDescEntity.getDecript())) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> itemGroupCompletableFuture = skuEntityCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 12.根据sku中的分类id、spuId以及skuId 查询组及组下的规格参数和值
            ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValue = this.pmsClient.queryGroupWithAttrValue(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVos = queryGroupWithAttrValue.getData();
            if (!CollectionUtils.isEmpty(itemGroupVos)) {
                itemVo.setGroups(itemGroupVos);
            }
        }, threadPoolExecutor);

        //allOf：所有11个任务都完成才执行新任务
        CompletableFuture.allOf(cateCompletableFuture,brandCompletableFuture,spuCompletableFuture,skuImagesCompletableFuture,
                itemCompletableFuture,wareCompletableFuture,saleAttrCompletableFuture,skuAttrCompletableFuture,
                stringCompletableFuture,spuDescCompletableFuture,itemGroupCompletableFuture).join();

        System.out.println("itemVo = " + itemVo);
        return itemVo;
    }
}
