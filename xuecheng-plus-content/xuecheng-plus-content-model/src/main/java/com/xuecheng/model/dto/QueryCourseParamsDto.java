package com.xuecheng.model.dto;

import lombok.Data;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年02月24日 下午 5:13
 */
@Data
public class QueryCourseParamsDto {
    //审核状态
    private String auditStatus;
    //课程名称
    private String courseName;
    //发布状态
    private String publishStatus;
}
