package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.TeachplanMedia;

import java.util.List;

public interface TeachplanService {
    public List<TeachplanDto> findTeachplayTree(Long courseId);
    /*
     * @description 只在课程计划
  * @param teachplanDto  课程计划信息
  * @return void
  * @author Mr.M
  * @date 2022/9/9 13:39
            */
    public void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * @description 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return com.xuecheng.content.model.po.TeachplanMedia
     * @author Mr.M
     * @date 2022/9/14 22:20
     */
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
