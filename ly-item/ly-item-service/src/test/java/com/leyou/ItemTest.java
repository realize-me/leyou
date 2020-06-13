package com.leyou;


import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
@SpringBootTest
public class ItemTest {

    @Autowired
    private CategoryService categoryService;
    
    @Test
    public void categoryTest(){
        List<Category> list = categoryService.findCategoryByParentId(0L);
        System.out.println(list.get(0));
    }



    @Test
    public void setGetTest(){
        SpecParam specParam = new SpecParam();
        specParam.setCid(1L);
        System.out.println(specParam);
    }
    
}
