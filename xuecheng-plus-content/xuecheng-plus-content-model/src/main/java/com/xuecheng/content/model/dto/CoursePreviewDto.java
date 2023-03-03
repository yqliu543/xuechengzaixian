package com.xuecheng.content.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年03月03日 上午 10:58
 */
@Data
public class CoursePreviewDto {
    //课程基本信息,课程营销信息
    CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;
}
