package com.atguigu.gmall.ums.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service("userService")
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 查询用户
     * username/phone/email    password
     */
    @Override
    public UserEntity queryUser(String loginName, String password) {
        // 1.根据登录名查询用户信息（拿到盐）
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>()
                .eq("username", loginName)
                .or()
                .eq("phone", loginName)
                .or()
                .eq("email", loginName));

        // 2.判断用户信息是否为空
        if (CollectionUtils.isEmpty(userEntities)){
            return null;
        }

        String pwd = null;
        for (UserEntity userEntity : userEntities) {
            // 3.获取该用户的盐，对用户输入的明文密码加盐加密
            pwd = DigestUtils.md5Hex(password + userEntity.getSalt());

            // 4.用数据库中的密码和加密后的密码比较
            if (StringUtils.equals(pwd, userEntity.getPassword())) {
                return userEntity;
            }
        }

        return null;
    }

    /**
     * 校验数据是否可用  (手机号 用户名 邮箱的唯一性)
     * data: 要校验的数据
     * type: 1.用户名 2.手机号 3.邮箱
     */
    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type) {
            case 1:wrapper.eq("username", data);break;
            case 2:wrapper.eq("phone", data);break;
            case 3:wrapper.eq("email", data);break;
            default:
                return null;
        }
        return this.count(wrapper) == 0;
    }

    /**
     * 注册功能  code 验证码
     */
    @Override
    public void register(UserEntity userEntity, String code) {
        // 1.校验短信验证码 TODO：

        // 2.生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);

        // 3.对密码加盐加密 (DigestUtils  apache提供的MD5加密)
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));

        // 4.新增用户
        userEntity.setLevelId(1l);
        userEntity.setSourceType(1);   // 来源
        userEntity.setIntegration(1000);  // 购物积分
        userEntity.setGrowth(1000);   // 赠送积分
        userEntity.setStatus(1);  // 状态
        userEntity.setCreateTime(new Date());  //注册时间

        this.save(userEntity);

        // 5.删除redis中的验证码 TODO：
    }

}