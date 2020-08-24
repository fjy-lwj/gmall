package com.atguigu.gmall.pms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 属性分组
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 11:50:57
 */
@Data
@TableName("pms_attr_group")
public class AttrGroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 分组id
	 */
	@TableId
	private Long id;
	/**
	 * 组名
	 */
	private String name;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 组图标
	 */
	private String icon;
	/**
	 * 所属分类id
	 */
	private Long categoryId;
	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 响应数据比AttrGroupEntity多出来一个字段，就是组下的属性列表attrEntities。所以要给AttrGroupEntity扩展一个字段，有两种扩展方式：
	 *
	 * 1. 直接在AttrGroupEntity中添加一个字段attrEntities
	 * 2. 编写一个扩展类GroupVo继承AttrGroupEntity，然后扩展一个attrEntities
	 */
	@TableField(exist = false)
	private List<AttrEntity> attrEntities;
}
