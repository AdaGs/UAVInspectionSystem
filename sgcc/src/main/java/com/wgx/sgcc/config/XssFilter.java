package com.gjdw.stserver.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gjdw.stserver.service.SetInfoService;
import com.gjdw.stserver.util.JsonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.reflect.internal.util.Origins;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
/*@Component
@WebFilter(urlPatterns = {"/*"},filterName = "XssFilter")*/
public class XssFilter implements Filter {

    @Autowired
    private SetInfoService setInfoService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    @Override
    public void destroy() {
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest res = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("X-Content-Type-Options","nosniff");//阻止浏览器做嗅探
        resp.setHeader("X-XSS-Protection","1; mode=block");//开启浏览器防XSS过滤器
        resp.setHeader("Set-Cookie","key=value;HttpOnly");//HttpOnly防止js劫取Cookie
        resp.setHeader("Content-Security-Policy","default-src 'self'");//防止 XSS
        resp.setHeader("Access-Control-Allow-Origin","*");
        resp.setHeader("Access-Control-Allow-Headers","Origin,X-Requested-With,Content-Type,Accept,Authorization,Referer");
        if (res.getRequestURI().equals("/api/safe/event/add") ||
            res.getRequestURI().equals("/api/cyberspacemap/scan/response") ||
            res.getRequestURI().equals("/api/penetration/scan/response") ||
            res.getRequestURI().equals("/api/bugcheck/scan/response")){
            chain.doFilter(res, resp);
            return;
        }
        String referer = res.getHeader("referer");
        String method = res.getMethod();
        List<String> refererUrls = Arrays.asList(setInfoService.getRefererUrls().split(";"));
        if (refererUrls.isEmpty()){
            resp.setContentType("application/json;charset=UTF-8");
            JsonResult jsonResult = new JsonResult();
            jsonResult.setError_code(-1);
            jsonResult.setError_msg("访问错误");
            String s = JSONObject.toJSONString(jsonResult);
            resp.getWriter().write(s);
            return;
        }
        if (referer == null){
            resp.setContentType("application/json;charset=UTF-8");
            JsonResult jsonResult = new JsonResult();
            jsonResult.setError_code(-1);
            jsonResult.setError_msg("访问错误");
            String s = JSONObject.toJSONString(jsonResult);
            resp.getWriter().write(s);
            return;
        }
        for (int i = 0; i < refererUrls.size(); i++) {
            if (!refererUrls.contains("*")) {
                if (!referer.startsWith(refererUrls.get(i))) {
                    if(refererUrls.size() == (i+1)) {
                        resp.setContentType("application/json;charset=UTF-8");
                        JsonResult jsonResult = new JsonResult();
                        jsonResult.setError_code(-1);
                        jsonResult.setError_msg("访问错误");
                        String s = JSONObject.toJSONString(jsonResult);
                        resp.getWriter().write(s);
                        return;
                    }
                    continue;
                }
                break;
            }
            break;
        }
        if(!method.equals("GET")){
            String origin = res.getHeader("origin");
            List<String> originUrls = Arrays.asList(setInfoService.getOriginUrls().split(";"));
            if (originUrls.isEmpty()){
                resp.setContentType("application/json;charset=UTF-8");
                JsonResult jsonResult = new JsonResult();
                jsonResult.setError_code(-1);
                jsonResult.setError_msg("访问错误");
                String s = JSONObject.toJSONString(jsonResult);
                resp.getWriter().write(s);
                return;
            }
            if (origin == null){
                resp.setContentType("application/json;charset=UTF-8");
                JsonResult jsonResult = new JsonResult();
                jsonResult.setError_code(-1);
                jsonResult.setError_msg("访问错误");
                String s = JSONObject.toJSONString(jsonResult);
                resp.getWriter().write(s);
                return;
            }
            for (int i = 0; i < originUrls.size(); i++) {
                if (!originUrls.contains("*")) {
                    if (!originUrls.contains(origin)) {
                        resp.setContentType("application/json;charset=UTF-8");
                        JsonResult jsonResult = new JsonResult();
                        jsonResult.setError_code(-1);
                        jsonResult.setError_msg("访问错误");
                        String s = JSONObject.toJSONString(jsonResult);
                        resp.getWriter().write(s);
                        return;
                    }
                    break;
                }
                break;
            }
        }
        //System.out.println("执行过滤操作");
        chain.doFilter(new XSSRequestWrapper(res), resp);
    }
}
