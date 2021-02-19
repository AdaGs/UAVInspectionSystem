package com.wgx.sgcc.requestLimit.filter;


import com.wgx.sgcc.requestLimit.annotation.RequestLimit;
import com.wgx.sgcc.requestLimit.exception.RequestLimitException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gs
 */
@Aspect
@Component
public class RequestLimitContract {
    private static final Logger logger = LoggerFactory.getLogger("requestLimitLogger");

    private Map<String,Integer> map = new HashMap<>();

    @Before("within(@org.springframework.stereotype.Controller *) && @annotation(limit)")
    public void requestLimit(final JoinPoint joinpoint , RequestLimit limit) throws RequestLimitException {
        Object[] args = joinpoint.getArgs();
        HttpServletRequest request = null;
        for (int i = 0; i < args.length; i++){
            if (args[i] instanceof HttpServletRequest){
                request = (HttpServletRequest) args[i];
                break;
            }
        }
        if (request == null){
            throw new RequestLimitException("方法缺失HttpServletRequest参数");
        }
        String ip = request.getLocalAddr();
        String url = request.getRequestURL().toString();
        String key = "req_limit".concat(url).concat(ip);
        if (map.get(key) == null || map.get(key) == 0){
            map.put(key,1);
        } else {
            map.put(key,map.get(key) + 1);
        }
        int count = map.get(key);

        if (count > limit.count()){
            logger.info("用户IP:"+ ip +"访问地址" + url+ "超过限定次数"+ limit.count() );
            throw new RequestLimitException();
        }
    }
}
