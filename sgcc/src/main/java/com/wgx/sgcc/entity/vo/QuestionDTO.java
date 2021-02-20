package com.btyc.faq.entity.vo;

import com.btyc.entity.dto.BaseDTO;
import lombok.Data;

/**
 * <p>
 * Description:
 * </p>
 *
 * @author zhaokang
 * @date 2020-09-08
 */
@Data
public class QuestionDTO extends BaseDTO {

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
