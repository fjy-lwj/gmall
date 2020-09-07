package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.gateway.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 2.局部过滤器
 * 需要到配置文件中配置
 * <p>
 * 此处验证是否登录 , 必须登录才放行  (过滤器)
 */

// 2.自定义过滤器工厂中指定Config泛型：KeyValueConfig
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory {

    @Autowired
    private JwtProperties jwtProperties;


    @Override
    public GatewayFilter apply(Object config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                return null;
            }
        };
    }
}
