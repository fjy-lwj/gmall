package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.BaseAttrValueVo;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {
    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient smsClient;



    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    /*
    查询spu列表
    */
    @Override
    public PageResultVo querySpuByCidPage(Long cid, PageParamVo pageParamVo) {

        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        //如果cid为0,查询所有
        if (cid != 0) {
            wrapper.eq("category_id", cid);
        }
        //如果key不为空
        String key = pageParamVo.getKey();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(t -> t.like("name", key).or().eq("id", key));
        }

        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    /**
     * 大保存
     */
    @Override
    public void bigSave(SpuVo spuVo) {
        //1.保存spu相关信息
        //1.1 保存spu表
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        spuVo.setId(null);  //防止id注入,显示的设置id为null
        this.save(spuVo);
        //方便下面赋值
        Long spuId = spuVo.getId();


        // 1.2 保存spu描述表
        List<String> spuImages = spuVo.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {
            // new 一个描述表
            SpuDescEntity descEntity = new SpuDescEntity();
            //和上面同一个spuId (改了id策略 手动输入)
            descEntity.setSpuId(spuId);
            //使用工具类将list转为字符串
            descEntity.setDecript(StringUtils.join(spuImages, ","));
            // 保存到数据库
            this.spuDescMapper.insert(descEntity);
            System.out.println("descEntity = " + descEntity);
        }

        //测试seata 分布式事务框架
        // int i = 1 /0;

        // 1.3保存spu基本属性表 pms_spu_attr_value
        List<BaseAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(baseAttrValueVo -> {

                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(baseAttrValueVo, spuAttrValueEntity);
                //和上面同一个spuId  (传入值时 没有传spu_id)
                spuAttrValueEntity.setSpuId(spuId);
                spuAttrValueEntity.setSort(1);
                spuAttrValueEntity.setId(null);
                return spuAttrValueEntity;

            }).collect(Collectors.toList());
            //保存    mapper没有保存集合的方法,所以用service
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }

        // 2.保存sku相关信息
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        //2.1 保存sku表
        skus.forEach(skuVo -> {
            skuVo.setId(null);
            skuVo.setSpuId(spuId);
            skuVo.setBrandId(spuVo.getBrandId());
            skuVo.setCatagoryId(spuVo.getCategoryId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                // 如果页面没有上传默认图片,就取第一张图片做默认图片 ,有就用上传的图片
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ? skuVo.getDefaultImage() : images.get(0));
                //skuVo.setDefaultImage(skuVo.getDefaultImage() == null ? images.get(0) : skuVo.getDefaultImage());
            }
            this.skuMapper.insert(skuVo);
            //方便下面赋值
            Long skuId = skuVo.getId();

            //2.2 保存sku图片表
            if (!CollectionUtils.isEmpty(images)) {
                List<SkuImagesEntity> skuImagesEntites = images.stream().map(image -> {
                    // 创建 图片表
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();

                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSort(1);
                    //设置默认图片的状态 0不是默认图  1是默认图
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(image, skuVo.getDefaultImage()) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                // 批量保存
                this.skuImagesService.saveBatch(skuImagesEntites);
                System.out.println("skuImagesEntites = " + skuImagesEntites);
            }

            //2.3 保存sku销售属性表  pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(saleAttr -> {
                    saleAttr.setSkuId(skuId);
                    saleAttr.setSort(0);
                    saleAttr.setId(null);
                });
                // 保存
                this.skuAttrValueService.saveBatch(saleAttrs);
            }

            // 3.营销sku相关信息
            // 3.1 保存积分信息
            // 3.2 保存满减信息
            // 3.3 保存打折信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.smsClient.saveSkuSales(skuSaleVo);

        });



    }

}