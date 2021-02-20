package com.btyc.faq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.btyc.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;


/**
 * 问题管理 Entity
 *
 * @author zhaokang
 * @version 2020-09-01
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("busi_faqs_questions")
public class QuestionEntity extends BaseEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 问题内容
     */
    private String content;

    /**
     * 积分值
     */
    private int integralVal;

    /**
     * 问题类型
     */
    private int type;

    /**
     * 问题状态
     */
    private int status;

    public void setId(String id) {
        this.id = UUID.randomUUID().toString();
    }
}
