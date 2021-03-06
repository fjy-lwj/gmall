package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallSmsApi {
    /** pms SpuService远程调用
     * 大保存
     */
    @PostMapping("sms/skubounds/sku/sales")
    public ResponseVo<Object> saveSkuSales(@RequestBody SkuSaleVo skuSaleVo);

    /*  商品详情页需要的数据接口：6
    根据skuId查询sku所有的优惠信息 sms的三张表
   */
    @GetMapping("sms/skubounds/sku/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querysalesByskuId(@PathVariable("skuId") Long skuId);

    /**
     *
     * 根据skuId查询购物积分,成长积分
     */
    @GetMapping("sms/skubounds/bounds/{skuId}")
    public ResponseVo<SkuBoundsEntity> queryboundsBySkuId(@PathVariable("skuId") Long skuId);



}
