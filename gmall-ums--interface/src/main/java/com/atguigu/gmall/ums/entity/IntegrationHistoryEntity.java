package com.atguigu.gmall.ums.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 购物积分记录表
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 12:40:56
 */
@Data
@TableName("ums_integration_history")
public class IntegrationHistoryEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * member_id
	 */
	private Long userId;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 变动数量
	 */
	private Integer count;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 来源
	 */
	private Integer sourceType;

}
