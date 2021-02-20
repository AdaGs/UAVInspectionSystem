package com.btyc.faq.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 问题VO
 *
 * @author zhaokang
 * @date 2020-09-04
 */

@Data
public class QuestionVO implements Serializable {
    /**
     * id
     */
    private Integer id;

    /**
     * 内容
     */
    private String content;

    /**
     * 积分值
     */
    private int integralVal;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
