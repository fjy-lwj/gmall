package com.atguigu.gmall.sms.controller;

import java.util.List;

import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 商品spu积分设置
 *
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 11:55:47
 */
@Api(tags = "商品spu积分设置 管理")
@RestController
@RequestMapping("sms/skubounds")
public class SkuBoundsController {

    @Autowired
    private SkuBoundsService skuBoundsService;

    /**
     *
     * 根据skuId查询购物积分,成长积分
     */
    @GetMapping("sms/skubounds/bounds/{skuId}")
    public ResponseVo<SkuBoundsEntity> queryboundsBySkuId(@PathVariable("skuId") Long skuId){
        SkuBoundsEntity skuBoundsEntity = this.skuBoundsService.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id",skuId));
        return ResponseVo.ok(skuBoundsEntity);
    }


    /*根据skuId查询sku所有的优惠信息 sms的三张表*/
    @GetMapping("sku/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querysalesByskuId(@PathVariable("skuId") Long skuId){
        List<ItemSaleVo> itemSaleVos = this.skuBoundsService.querysalesByskuId(skuId);
        return ResponseVo.ok(itemSaleVos);
    }



    /** pms SpuService远程调用
     * 大保存
     */
    @PostMapping("sku/sales")
    public ResponseVo<Object> saveSkuSales(@RequestBody SkuSaleVo skuSaleVo) {
        this.skuBoundsService.saveSkuSales(skuSaleVo);
        return ResponseVo.ok();
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuBoundsByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = skuBoundsService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuBoundsEntity> querySkuBoundsById(@PathVariable("id") Long id){
		SkuBoundsEntity skuBounds = skuBoundsService.getById(id);

        return ResponseVo.ok(skuBounds);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuBoundsEntity skuBounds){
		skuBoundsService.save(skuBounds);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuBoundsEntity skuBounds){
		skuBoundsService.updateById(skuBounds);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		skuBoundsService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
