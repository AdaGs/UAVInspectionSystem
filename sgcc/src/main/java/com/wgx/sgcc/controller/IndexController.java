package com.wgx.sgcc.controller;

import com.wgx.sgcc.config.JsonResult;
import com.wgx.sgcc.config.ValidCodeCache;
import com.wgx.sgcc.config.model.VerifyCode;
import com.wgx.sgcc.config.service.UserService;
import com.wgx.sgcc.config.service.ValidCodeService;
import com.wgx.sgcc.config.service.impl.ValidCodeServiceImpl;
import com.wgx.sgcc.model.qo.UserQo;
import com.wgx.sgcc.requestLimit.annotation.RequestLimit;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * @time
 * @author gs
 */
@RestController
@CrossOrigin(origins = {" * "},allowedHeaders = {" * "})
@Api(description = "用户")
@Slf4j
@RequestMapping(value = "/api")
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);



    @Autowired
    private UserService userService;

    @ApiOperation("登录")

    @RequestMapping(value = "login", method = RequestMethod.POST)
    public JsonResult index(@RequestBody(required = false) UserQo user){
        String verificationcode = user.getVerificationcode();
        String sessionKey =user.getTimestamp();
        String sessionvalue = (String) ValidCodeCache.getRealLink(sessionKey);
        if (sessionvalue == null || verificationcode == null) {
            return JsonResult.error(-1,"验证码不存在",null);
        }
        if (!verificationcode.toUpperCase(Locale.ENGLISH).equals(sessionvalue.toUpperCase(Locale.ENGLISH))) {
            return JsonResult.error(-1,"验证码错误",null);
        }
        Map<String,Object> loginMap = new HashMap<>();
        loginMap.put("userName",user.getUsername());
        loginMap.put("passWord",user.getPassword());
        userService.insert(loginMap,"login");
        return JsonResult.error(-1,"用户名或密码错误！",null);
    }

    @ApiOperation("注册")
    @RequestLimit(count = 5)
    @RequestMapping(value = "logon",method = RequestMethod.POST)
    public JsonResult logon(@RequestBody UserQo user, HttpServletRequest request){
        String verificationCode = user.getVerificationcode();
        String sessionKey = user.getTimestamp();
        String sessionvalue = (String) ValidCodeCache.getRealLink(sessionKey);
        if (sessionvalue == null || verificationCode == null) {
            return JsonResult.error(-1,"验证码不存在",null);
        }
        if (!verificationCode.toUpperCase(Locale.ENGLISH).equals(sessionvalue.toUpperCase(Locale.ENGLISH))) {
            return JsonResult.error(-1,"验证码错误",null);
        }
        String userName = user.getUsername();
        String passWord = user.getPassword();
        String phoneNumber = user.getPhonenumber();
        String email = user.getEmail();
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Map<String,Object> userMap = new HashMap<String,Object>();
        userMap.put("userName",userName);
        userMap.put("passWord",passWord);
        userMap.put("date",new Date());
        userMap.put("phoneNumber",phoneNumber);
        userMap.put("email",email);
        userService.insert(userMap,"user");
        return JsonResult.error(-1,"注册失败",null);

    }

    /**
     * 加载验证码
     * @param request
     * @param response
     */
    @ApiOperation("加载验证码")
    @RequestMapping(value = "/getVerifyCode",method = RequestMethod.GET)
    public void verifyCode(HttpServletRequest request, HttpServletResponse response) {
        String timestamp = request.getParameter("timestamp");

        try {
            ValidCodeService validCodeService = new ValidCodeServiceImpl();
            //设置长宽
            VerifyCode verifyCode = validCodeService.generate(80, 28);
            String code = verifyCode.getCode();
            //将VerifyCode绑定session
            //request.getSession().setAttribute(timestamp, code);
            ValidCodeCache.map.put(timestamp, code);
            //设置响应头
            response.setHeader("Pragma", "no-cache");
            //设置响应头
            response.setHeader("Cache-Control", "no-cache");
            //在代理服务器端防止缓冲
            response.setDateHeader("Expires", 0);
            //设置响应内容类型
            response.setContentType("image/jpeg");
            response.getOutputStream().write(verifyCode.getImgBytes());
            response.getOutputStream().flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }
    @RequestMapping(value = "/getCode",method = RequestMethod.POST)
    @ApiOperation("获取验证码")
    public JsonResult getCode(@RequestBody UserQo user, HttpServletRequest request) {
        String verificationCode = user.getVerificationcode();
        String sessionKey = user.getTimestamp();
        String sessionvalue = (String) ValidCodeCache.getRealLink(sessionKey);
        String userName = user.getUsername();
        String passWord = user.getPassword();
        String phoneNumber = user.getPhonenumber();
        String email = user.getEmail();
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Object> userMap = new HashMap<String, Object>();
        userMap.put("userName", userName);
        userMap.put("passWord", passWord);
        userMap.put("date", format.format(date));
        userMap.put("phoneNumber", phoneNumber);
        userMap.put("email", email);
        userService.insert(userMap, "user");
        return JsonResult.returnSuccess("验证码发送成功",null);
    }
}
