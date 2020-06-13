package com.leyou.item.web;

import com.leyou.common.dto.CartDTO;
import com.leyou.item.service.GoodsService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
class GoodsControllerTest {
    @Autowired
    private GoodsService goodsService;
    @Test
    void decreaseStock() {
        List<CartDTO> cartDTOList = Arrays.asList(new CartDTO(2600242L, 2), new CartDTO(2600248L, 2));
        goodsService.decreaseStock(cartDTOList);
    }
}