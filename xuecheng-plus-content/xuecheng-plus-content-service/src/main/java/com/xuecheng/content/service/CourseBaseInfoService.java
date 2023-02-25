package com.xuecheng.content.service;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
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
}
