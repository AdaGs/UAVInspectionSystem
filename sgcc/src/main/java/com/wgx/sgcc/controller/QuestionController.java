package com.btyc.faq.controller;


import com.btyc.entity.bean.PageBean;
import com.btyc.entity.bean.ResultBean;
import com.btyc.exception.GlobalException;
import com.btyc.faq.entity.QuestionEntity;
import com.btyc.faq.entity.vo.QuestionDTO;
import com.btyc.faq.entity.vo.QuestionVO;
import com.btyc.faq.entity.vo.UpdateQuesDTO;
import com.btyc.faq.service.IQuestionService;
import com.btyc.utils.PageBeanFactory;
import com.btyc.utils.ResultBeanFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


/**
 * 问题管理 Controller
 *
 * @author zhaokang
 * @version 2020-09-01
 */
@Slf4j
@RestController
@Api(description = "问题管理")
public class QuestionController {

    /**
     * 问题管理 Service
     */
    @Resource
    private IQuestionService questionService;

    /**
     * 获取问题列表
     */
    @GetMapping("getQuestionList")
    @ApiOperation("测试是否能访问")
    public PageBean getQuestionList() {
        log.info("成功访问!");
        List<QuestionEntity> qList = questionService.getQuestionList();
        return PageBeanFactory.getSuccess(qList, 2);
    }

    /**
     * 测试1
     */
    @GetMapping("/test1/{id}")
    @ApiOperation("测试1")
    public ResultBean test1(@PathVariable("id") Long id) {
        log.info("test成功访问!");
        return ResultBeanFactory.getFailed();


    }

    /**
     * 测试2
     */
    @GetMapping("/test2/{id}")
    @ApiOperation("测试2")
    public ResultBean test2(@PathVariable("id") Long id) {
        return ResultBeanFactory.getSuccess();
    }

    /**
     * 测试3
     */
    @GetMapping("/test3/{id}")
    @ApiOperation("测试3")
    public ResultBean test3(@PathVariable("id") Long id) {
        log.info("测试3接收到的id：{}", id);
        throw new GlobalException(400, "怎么会这样？？？");
    }

    /**
     * 测试4
     */
    @GetMapping("/test4/{id}")
    @ApiOperation("测试4")
    public ResultBean test4(@PathVariable("id") Long id) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setContent("测试一哈返回值");
        log.info("测试4接收到的id：{}", id);
        return ResultBeanFactory.getSuccess(questionEntity);
    }

    /**
     * 测试4
     */
    @GetMapping("/getQuestionById/{id}")
    @ApiOperation("根据id，查询问题")
    public ResultBean getQuestionById(@PathVariable("id") Integer id) {
        log.info("测试4接收到的id：{}", id);
        QuestionVO questionVO = questionService.getQuestionById(id);
        return ResultBeanFactory.getSuccess(questionVO);
    }

    /**
     * 测试5
     */
    @PostMapping("/addQuestion")
    @ApiOperation("根据id，查询问题")
    public ResultBean getQuestionById(@RequestBody QuestionDTO questionDTO) {
        log.info("提问问题：{}", questionDTO);
        if (!Objects.isNull(questionDTO)) {
            QuestionEntity questionEntity = new QuestionEntity();
            BeanUtils.copyProperties(questionDTO, questionEntity);
            log.info("生成的id为：{}", questionEntity.getId());
            return questionService.addQuestion(questionEntity) > 0 ? ResultBeanFactory.getSuccess() : ResultBeanFactory.getFailed();
        } else {
            throw new GlobalException(400, "对象为空！");
        }
    }

    /**
     * 测试6
     */
    @PostMapping("/updateQuestion")
    @ApiOperation("根据id，修改问题")
    public ResultBean updateQuestionById(@RequestBody UpdateQuesDTO questionDTO) {
        return ResultBeanFactory.getSuccess();
    }
}
