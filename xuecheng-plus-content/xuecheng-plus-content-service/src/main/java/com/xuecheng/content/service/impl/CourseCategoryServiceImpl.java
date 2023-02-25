package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        ArrayList<CourseCategoryTreeDto> result = new ArrayList<>();
        HashMap<String, CourseCategoryTreeDto> nodeMap = new HashMap<>();
        courseCategoryTreeDtos.stream().forEach(item->{
            nodeMap.put(item.getId(),item);
            if (item.getParentid().equals(id)){
                result.add(item);
            }
            String parentid = item.getParentid();
            CourseCategoryTreeDto parentNode = nodeMap.get(parentid);
            if (parentNode!=null){
                List childrenTreeNodes = parentNode.getChildrenTreeNodes();
                if (childrenTreeNodes==null){
                    parentNode.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                parentNode.getChildrenTreeNodes().add(item);
            }
        });
        return result;
    }
}
