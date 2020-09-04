package com.atguigu.gmall.search.listener;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.sound.midi.VoiceStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ItemListener {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "PMS-SAVE-QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS-ITEM-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener1(Long squId, Channel channel, Message message) throws IOException {

        try {
            //根据squId获取spu信息
            ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(squId);
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            //非空验证
            if (spuEntity == null) {
                return;
            }

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
                        goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b)->(a+b)).get().intValue());
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
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            e.printStackTrace();
            // 判读有无重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过就拒绝
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            } else {
                // 没就重试一次
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }
        }
    }

}
