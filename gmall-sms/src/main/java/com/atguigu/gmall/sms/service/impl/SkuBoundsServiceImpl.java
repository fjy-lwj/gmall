package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.util.CollectionUtils;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuFullReductionMapper fullReductionMapper;
    @Autowired
    private SkuLadderMapper ladderMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    /** pms SpuService远程调用
     * 大保存
     */
    @Override
    public void saveSkuSales(SkuSaleVo skuSaleVo) {
        // 3.1 保存积分信息
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo, skuBoundsEntity);
        // 还有个work [1,1,0,0] 从右往左 转为10进制integer存
        List<Integer> work = skuSaleVo.getWork();
        if (!CollectionUtils.isEmpty(work) && work.size() == 4) {
            skuBoundsEntity.setWork(work.get(3) * 8 + work.get(2) * 4 + work.get(1) * 2 + work.get(0) * 1);
        }
        this.save(skuBoundsEntity);
        // 3.2 保存满减信息
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo, skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.fullReductionMapper.insert(skuFullReductionEntity);
        // 3.3 保存打折信息
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo, skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.ladderMapper.insert(skuLadderEntity);
    }

    /*根据skuId查询sku所有的优惠信息 sms的三张表*/
    @Override
    public List<ItemSaleVo> querysalesByskuId(Long skuId) {
        ArrayList<ItemSaleVo> itemSaleVos = new ArrayList<>();
        //1.查询积分优惠
        SkuBoundsEntity skuBoundsEntity = this.baseMapper.selectOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null) {
            ItemSaleVo boundsSaleVo = new ItemSaleVo();
            boundsSaleVo.setType("积分");
            boundsSaleVo.setDesc("送" + skuBoundsEntity.getGrowBounds() + "成长积分，送" + skuBoundsEntity.getBuyBounds() + "购物积分");
            itemSaleVos.add(boundsSaleVo);
        }
        // 2.查询满减优惠
        SkuFullReductionEntity reductionEntity = this.fullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (reductionEntity != null) {
            ItemSaleVo skuFullReductionSaleVo = new ItemSaleVo();
            skuFullReductionSaleVo.setType("满减");
            skuFullReductionSaleVo.setDesc("满" + reductionEntity.getFullPrice() + "减" + reductionEntity.getReducePrice());
            itemSaleVos.add(skuFullReductionSaleVo);
        }
        // 3.查询打折优惠
        SkuLadderEntity ladderEntity = this.ladderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (ladderEntity != null) {
            ItemSaleVo ladderSaleVo = new ItemSaleVo();
            ladderSaleVo.setType("打折");
            ladderSaleVo.setDesc("满" + ladderEntity.getFullCount() + "件，打" + ladderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            itemSaleVos.add(ladderSaleVo);
        }
        return itemSaleVos;
    }

}