package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 跳转到login页面
     */
    @GetMapping("toLogin")
    public String toLogin(@RequestParam(value = "returnUrl", required = false) String returnUrl, Model model) {
        //获取登录时的路径
        //String requestURI = request.getRequestURI();
        model.addAttribute("returnUrl", returnUrl);
        return "login";
    }

    /**
     * 登录
     */
    @PostMapping("login")
    public String login(@RequestParam(value = "returnUrl",required = false) String returnUrl,
                        @RequestParam(value = "loginName") String loginName,
                        @RequestParam(value = "password") String password,
                        HttpServletRequest request,
                        HttpServletResponse response) throws Exception {

        this.authService.accredit(loginName,password,request,response);
        //重定向回到原来的地址
        if (StringUtils.isNotBlank(returnUrl)) {
            return "redirect:" + returnUrl;
        }

        return "redirect:http://gmall.com";
    }

}
