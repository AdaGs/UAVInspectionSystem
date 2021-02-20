package com.btyc.faq.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btyc.faq.entity.QuestionEntity;
import com.btyc.faq.entity.vo.QuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 问题管理 mapper
 *
 * @author zhaokang
 * @version 2020-09-01
 */
@Mapper
public interface QuestionMapper extends BaseMapper<QuestionEntity> {

    /**
     * 根据id，查询问题
     *
     * @param id
     * @return
     */
    QuestionVO getQuestionById(@Param("id") Integer id);
}
