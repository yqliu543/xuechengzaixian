package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
*@description
*@return
*@author lyq
*@date 2023/2/25 17:23
*/
@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    private  CourseBaseMapper courseBaseMapper;

    @Override
   /**
   *@description
   *@param params
    * @param queryCourseParamsDto
   *@return com.xuecheng.base.model.PageResult<com.xuecheng.content.model.po.CourseBase>
   *@author lyq
   *@date 2023/2/25 17:30
   */
    public PageResult<CourseBase> querCourseBaseList(PageParams params, QueryCourseParamsDto queryCourseParamsDto) {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据课程名称模糊查询 name like ‘%名称%’
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        //根据审核状态模糊查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态模糊查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());

        //分页参数
        Page<CourseBase> page = new Page<>(params.getPageNo(), params.getPageSize());
        //分页查询
        Page<CourseBase> pageresult = courseBaseMapper.selectPage(page, queryWrapper);
        long total = pageresult.getTotal();
        List<CourseBase> records = pageresult.getRecords();
        //返回数据
        PageResult<CourseBase> result = new PageResult<>(records, total, params.getPageNo(), params.getPageSize());
        return result;
    }
}
