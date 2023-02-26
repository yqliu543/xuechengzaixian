package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private CourseMarketServiceImpl courseMarketService;

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


    /**
    *@description
    *@param companyId
     * @param addCourseDto
    *@return com.xuecheng.content.model.dto.CourseBaseInfoDto
    *@author lyq
    *@date 2023/2/26 12:13
    */
    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {

        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(addCourseDto,courseBase);
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        int insert = courseBaseMapper.insert(courseBase);
        Long courseId = courseBase.getId();
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto,courseMarket);
        courseMarket.setId(courseId);
        int insert1 = courseMarketMapper.insert(courseMarket);
        if (insert1<=0||insert<=0){
            throw new RuntimeException("添加课程失败");
        }
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if (courseMarket!=null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        String st = courseBase.getSt();
        String mt = courseBase.getMt();
        CourseCategory stcourseCategory = courseCategoryMapper.selectById(st);
        CourseCategory mtcourseCategory = courseCategoryMapper.selectById(mt);
        if (stcourseCategory!=null){
            String name = stcourseCategory.getName();
            courseBaseInfoDto.setStName(name);
        }
        if (mtcourseCategory!=null){
            String name = mtcourseCategory.getName();
            courseBaseInfoDto.setMtName(name);
        }
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto) {
        Long id = dto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase==null){
            XueChengPlusException.cast("课程不存在");
        }
        if (!courseBase.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("本机构只能修改本机构");
        }
        BeanUtils.copyProperties(dto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
        courseBaseMapper.updateById(courseBase);
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);
        boolean saveOrUpdate = courseMarketService.saveOrUpdate(courseMarket);
        CourseBaseInfoDto courseBaseInfo = this.getCourseBaseInfo(id);
        return courseBaseInfo;
    }
}
