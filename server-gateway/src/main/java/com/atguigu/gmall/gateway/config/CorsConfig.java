package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 解决跨域
 */



@Configuration
public class CorsConfig {
    

    @Bean
    public CorsWebFilter corsWebFilter(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedHeader("*");//设置响应给浏览器的消息头  *代表所有
        corsConfiguration.addAllowedOrigin("*");//设置允许访问的网络  *代表所有
        corsConfiguration.setAllowCredentials(true);//设置是否从服务器获取cookie
        corsConfiguration.addAllowedMethod("*");//设置任意请求方式   *代表所有

        //设置源对象
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);

        return new CorsWebFilter(corsConfigurationSource);
    }





}
