package com.xuecheng.content.service;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

public interface CourseBaseInfoService {
    /**
    *@description  课程查询
    *@param params
     * @param queryCourseParamsDto
    *@return com.xuecheng.base.model.PageResult<com.xuecheng.content.model.po.CourseBase>
    *@author lyq
    *@date 2023/2/25 17:21
    */
    PageResult<CourseBase> querCourseBaseList(PageParams params,QueryCourseParamsDto queryCourseParamsDto);
    /**
    *@description 课程添加
    *@param companyId
     * @param addCourseDto
    *@return com.xuecheng.content.model.dto.CourseBaseInfoDto
    *@author lyq
    *@date 2023/2/26 12:29
    */
    CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto addCourseDto);

    /**
    *@description 根据课程id查询
    *@param courseId
    *@return com.xuecheng.content.model.dto.CourseBaseInfoDto
    *@author lyq
    *@date 2023/2/26 12:30
    */
    CourseBaseInfoDto getCourseBaseInfo(Long courseId);


    /**
     * @description 修改课程信息
     * @param companyId  机构id
     * @param dto  课程信息
     * @return com.xuecheng.content.model.dto.CourseBaseInfoDto
     * @author Mr.M
     * @date 2022/9/8 21:04
     */
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto);


}
