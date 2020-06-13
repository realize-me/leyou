package com.leyou.order.client;

import com.leyou.order.dto.AddressDTO;

import java.util.ArrayList;
import java.util.List;

public abstract class AddressClient {
    public static final List<AddressDTO> addressDTOList = new ArrayList<AddressDTO>() {
        {
            AddressDTO address = new AddressDTO();
            address.setId(1L);
            address.setState("上海");
            address.setCity("上海市");
            address.setDistrict("浦东新区");
            address.setAddress("航空镇航头路18号传智播客");
            address.setName("虎哥");
            address.setPhone("13511112222");
            address.setIsDefault(true);
            address.setZipCode("210000");
            add(address);

            AddressDTO address2 = new AddressDTO();
            address2.setId(2L);
            address2.setState("河北省");
            address2.setCity("石家庄市");
            address2.setDistrict("裕华区");
            address2.setAddress("珠峰大街 288号");
            address2.setName("张三");
            address2.setPhone("1360001111");
            address2.setIsDefault(true);
            address2.setZipCode("320000");
            add(address2);
        }
    };

    public static AddressDTO findById(Long id) {
        for (AddressDTO addressDTO : addressDTOList) {
            if (addressDTO.getId()==id) return addressDTO;
        }
        return null;
    }

}
