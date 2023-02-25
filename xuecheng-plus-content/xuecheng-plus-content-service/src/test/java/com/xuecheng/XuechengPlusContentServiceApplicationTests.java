package com.xuecheng;

import com.xuecheng.content.ContentApplication;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.po.CourseBase;
import io.swagger.annotations.ApiModelProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {ContentApplication.class})
class XuechengPlusContentServiceApplicationTests {

    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Test
    void testcontextLoads() {
        CourseBase courseBase = courseBaseMapper.selectById(22);
        System.out.println(courseBase);
    }

}
