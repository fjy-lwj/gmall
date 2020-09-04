package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 11:50:57
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 根据三级分类id查询分组及组下的规格参数
     */
    List<AttrGroupEntity> queryGroupWithAttrsByCid(Long catId);

    /**
     * 根据分类id结合spuId或者skuId查询组及组下规格参数与值
     */
    List<ItemGroupVo> queryGroupWithAttrValue(Long cid, Long spuId, Long skuId);

}

