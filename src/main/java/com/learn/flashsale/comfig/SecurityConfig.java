package com.learn.flashsale.comfig;
import com.learn.flashsale.filiter.JwtAuthenticationTokenFilter;
import com.learn.flashsale.handler.AuthenticationEntryPointImpl;
import com.learn.flashsale.handler.CustomAuthenticationFailureHandler;
import com.learn.flashsale.propoties.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    ConfigProperties configProperties;
    @Autowired
    JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    @Autowired
    AuthenticationEntryPointImpl authenticationEntryPoint;
    @Autowired
    CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
//todo 认证管理器和过滤器得学，读取配置文件无权路径报错修改

    /**
     * 配置认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration, BCryptPasswordEncoder bCryptPasswordEncoder) throws Exception {
        //为安全管理器配置认证提供者，加入自定义的UsernamePasswordAuthenticationProvider，他使用list保存provider的所以可以添加多个provider
        //为usernamePasswordAuthenticationProvider设置密码验证器
        //可以继续添加其他的provider
//        authenticationManagerBuilder.authenticationProvider(phoneNumberAuthenticationProvider);
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 配置认证过滤器
     */
    @Bean
    public UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        UsernamePasswordAuthenticationFilter authenticationFilter = new UsernamePasswordAuthenticationFilter();
        //设置认证管理器
        authenticationFilter.setAuthenticationManager(authenticationManager);
        //设置登录成功和失败的处理器，分别对应AuthenticationSuccessHandler和AuthenticationFailureHandler，自行实现里面的方法就行，认证成功或失败时AbstractAuthenticationProcessingFilter会帮我们调用这两个处理器
//        authenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
//        authenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        //改变登录页面的url，过滤器写了默认的，就可以不用加
//        authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/user/login", "POST"));
        //必须是post请求
        authenticationFilter.setPostOnly(true);
        return authenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter, AuthenticationManager authenticationManager) throws Exception {
        httpSecurity
                .cors().and()
                //关闭csrf  前后端分离项目不必担心csrf攻击
                .csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeHttpRequests(
                        registry ->
                                registry
                                        //放行的请求
                                        .requestMatchers(new AntPathRequestMatcher("/users/login", "POST"), new AntPathRequestMatcher("/users/register", "POST")).permitAll()
                                        .anyRequest()
                                        .authenticated()
                )
                // 将我们自己的usernamePasswordAuthenticationFilter添加到SpringSecurity的过滤器链中
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(customAuthenticationFailureHandler).and()
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
