package com.wgx.sgcc.config;


import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * SwaggerConfig
 *
 * @author zhaokang
 * @version 2020-09-01
 */
@Configuration
@EnableSwagger2
public class  SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                //设置扫描选择器
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
//                .paths(PathSelectors.none())//线上环境关闭swagger
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                //文档内容配置信息
                .title("网络安全自动化渗透平台")
                .description("接口文档")
                .termsOfServiceUrl("https://swagger.io")
                .version("1.0")
                .build();
    }
}