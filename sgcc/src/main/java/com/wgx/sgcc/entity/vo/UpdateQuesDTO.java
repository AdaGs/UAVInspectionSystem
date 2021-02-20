package com.btyc.faq.entity.vo;

import com.btyc.entity.dto.BaseDTO;

/**
 * <p>
 * Description:
 * </p>
 *
 * @author zhaokang
 * @date 2020-09-11
 */
public class UpdateQuesDTO extends BaseDTO {

    private String id;

    /**
     * 内容
     */
    private String content;

    /**
     * 积分值
     */
    private int integralVal;

    /**
     * 类型
     */
    private int type;

}
