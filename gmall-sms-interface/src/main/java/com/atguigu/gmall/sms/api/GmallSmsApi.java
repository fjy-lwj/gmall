package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallSmsApi {
    /** pms SpuService远程调用
     * 大保存
     */
    @PostMapping("sms/skubounds/sku/sales")
    public ResponseVo<Object> saveSkuSales(@RequestBody SkuSaleVo skuSaleVo);


}
