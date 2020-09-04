package com.atguigu.gmall.wms.api;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 属于wms
 * 专门提供api接口(feign)
 * 由调用者继承
 */

public interface GmallWmsApi {

    /**数据导入es 第三个接口      商品详情页需要的数据接口：7
     * 根据skuId查询库存信息
     */
    @GetMapping("wms/waresku/sku/{skuId}")
    public ResponseVo<List<WareSkuEntity>> queryWareSkuBySkuId(@PathVariable("skuId") Long skuId);


}
