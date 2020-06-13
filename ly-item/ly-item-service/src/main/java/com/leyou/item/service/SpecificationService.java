package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;


    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> groups = specGroupMapper.select(specGroup);
        if(CollectionUtils.isEmpty(groups)){
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }

        return groups;
    }

    public void addGroup(SpecGroup specGroup) {
        int count = specGroupMapper.insert(specGroup);
        if(count!=1){
            throw new LyException(ExceptionEnum.SPEC_GROUP_SAVE_ERROR);
        }
    }

    public void editGroup(SpecGroup specGroup) {
        int count = specGroupMapper.updateByPrimaryKey(specGroup);
        if(count!=1){
            throw new LyException(ExceptionEnum.SPEC_GROUP_SAVE_ERROR);
        }
    }

    public void deleteGroup(Long id) {
        int count = specGroupMapper.deleteByPrimaryKey(id);
        if(count!=1){
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
    }

    public List<SpecParam> queryParamList(Long gid, Long cid, Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);

        List<SpecParam> params = specParamMapper.select(specParam);

        if(CollectionUtils.isEmpty(params)){
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        return params;
    }

    public void addParam(SpecParam specParam) {
        int count = specParamMapper.insert(specParam);
        if(count!=1){
            throw new LyException(ExceptionEnum.SPEC_PARAM_SAVE_ERROR);
        }
    }

    public void editParam(SpecParam specParam) {
        int count = specParamMapper.updateByPrimaryKey(specParam);
        if(count!=1){
            throw new LyException(ExceptionEnum.SPEC_PARAM_SAVE_ERROR);
        }
    }

    public void deleteParam(Long id) {
        int count = specParamMapper.deleteByPrimaryKey(id);
        if(count!=1){
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
    }

    public List<SpecGroup> queryListByCid(Long cid) {
        // 查询指定分类下的规格参数组
        List<SpecGroup> specGroups = queryGroupsByCid(cid);
        // 查询指定分类下的规格参数
        List<SpecParam> specParams = queryParamList(null, cid, null);
        // 将规格参数封装到规格参数组里面
        Map<Long, List<SpecParam>> map = new HashMap<>();
        for (SpecParam specParam : specParams) {
            if(!map.containsKey(specParam.getGroupId())){
                map.put(specParam.getGroupId(),new ArrayList<>());
            }
            map.get(specParam.getGroupId()).add(specParam);
        }
        for (SpecGroup specGroup : specGroups) {
            specGroup.setParams(map.get(specGroup.getId()));
        }
        return specGroups;
    }
}
