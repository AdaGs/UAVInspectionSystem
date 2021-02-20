package com.btyc.faq.service;

import com.btyc.faq.entity.QuestionEntity;
import com.btyc.faq.entity.vo.QuestionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 问题管理 Service
 *
 * @author zhaokang
 * @version 2020-09-01
 */
public interface IQuestionService {

    /**
     * @param id
     * @return
     */
    QuestionVO getQuestionById(Integer id);

    /**
     * 新增问题
     *
     * @param questionEntity 问题对象
     * @return
     */
    int addQuestion(QuestionEntity questionEntity);


    /**
     * 修改问题
     * @param questionEntity
     * @return
     */
    int updateQuestion(QuestionEntity questionEntity);

    /**
     * @return
     */
    List<QuestionEntity> getQuestionList();
}
