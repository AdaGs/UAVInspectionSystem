package com.btyc.faq.service.impl;

import com.btyc.faq.entity.QuestionEntity;
import com.btyc.faq.entity.vo.QuestionVO;
import com.btyc.faq.mapper.QuestionMapper;
import com.btyc.faq.service.IQuestionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 问题管理 ServiceImpl
 *
 * @author zhaokang
 * @version 2020-09-01
 */
@Service("questionService")
public class QuestionServiceImpl implements IQuestionService {

    /**
     * 问题管理 Mapper
     */
    @Resource
    private QuestionMapper questionMapper;


    /**
     * @param id
     * @return
     */
    @Override
    public QuestionVO getQuestionById(Integer id) {
        return questionMapper.getQuestionById(id);
    }


    /**
     * 新增问题
     *
     * @param questionEntity 问题对象
     * @return
     */
    @Override
    public int addQuestion(QuestionEntity questionEntity) {
        return questionMapper.insert(questionEntity);
    }


    /**
     *
     * xiugai
     * @param questionEntity
     * @return
     */
    @Override
    public int updateQuestion(QuestionEntity questionEntity) {
        return questionMapper.updateById(questionEntity);
    }


    /**
     * @return
     */
    @Override
    public List<QuestionEntity> getQuestionList() {
        return questionMapper.selectList(null);
    }
}
