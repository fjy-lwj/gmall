package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model) {
        //查询一级分类
        List<CategoryEntity> categoryEntityList = this.indexService.queryLv1lCategories();
        model.addAttribute("categories", categoryEntityList);
        return "index";
    }

    //查询二级分类  携带三级分类
    @GetMapping("index/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubByPid(@PathVariable("pid")Long pid) {
        List<CategoryEntity> categoryEntities = this.indexService.queryCategoriesWithSubByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }


    // 测试redis 实现分布式锁
    @GetMapping("index/testLock")
    @ResponseBody
    public ResponseVo<Object> testLock() {
        this.indexService.testLock();
        return ResponseVo.ok();
    }

    // 测试redis 实现自动续期 (测试成功)
    @GetMapping("index/testLock2")
    @ResponseBody
    public ResponseVo<Object> testLock2() {
        this.indexService.testLock2();
        return ResponseVo.ok();
    }
}
