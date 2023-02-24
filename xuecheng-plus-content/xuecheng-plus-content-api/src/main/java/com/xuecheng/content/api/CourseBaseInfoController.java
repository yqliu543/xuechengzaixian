package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.model.dto.QueryCourseParamsDto;
import com.xuecheng.model.po.CourseBase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年02月24日 下午 5:40
 */
@Api(value = "课程管理相关接口",tags = "课程管理相关接口")
@RestController
public class CourseBaseInfoController {
    @PostMapping("/content/list")
    @ApiOperation("课程查询接口")
    public PageResult<CourseBase> list(PageParams params, @RequestBody QueryCourseParamsDto queryCourseParamsDto){
        return null;
    }
}
