package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.model.dto.QueryCourseParamsDto;
import com.xuecheng.model.po.CourseBase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年02月24日 下午 5:40
 */
@RestController
public class CourseBaseInfoController {
    @PostMapping("/content/list")
    public PageResult<CourseBase> list(PageParams params, @RequestBody QueryCourseParamsDto queryCourseParamsDto){
        return null;
    }
}
