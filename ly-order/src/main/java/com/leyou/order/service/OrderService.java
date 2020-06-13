package com.leyou.order.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.util.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper detailMapper;
    @Autowired
    private OrderStatusMapper statusMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private PayHelper payHelper;

    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        Order order = new Order();
        // 生成订单id
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        // 用户信息
        UserInfo userInfo = UserInterceptor.getUserInfo();
        order.setUserId(userInfo.getId());
        order.setBuyerNick(userInfo.getUsername());
        order.setBuyerRate(false);

        // 收货人信息
        AddressDTO address = AddressClient.findById(orderDTO.getAddressId());
        order.setReceiver(address.getName());
        order.setReceiverAddress(address.getAddress());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverMobile(address.getPhone());
        order.setReceiverState(address.getState());
        order.setReceiverZip(address.getZipCode());
        // 金额
        // 将CartList封装为一个map，key是sku的id，value是sku的数量
        Map<Long, Integer> cartMap = orderDTO.getCarts().stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        // 获取所有sku的id
        Set<Long> ids = cartMap.keySet();
        // 批量查询sku
        List<Sku> skuList = goodsClient.querySkuByIds(new ArrayList<>(ids));
        // 遍历skuList，获取其价格
        long totalPay = 0L;
        // 准备detailList
        ArrayList<OrderDetail> details = new ArrayList<>();
        for (Sku sku : skuList) {
            // 计算总额
            totalPay += sku.getPrice() * cartMap.get(sku.getId());
            // 封装orderDetail
            OrderDetail detail = new OrderDetail();
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detail.setNum(cartMap.get(sku.getId()));
            detail.setOrderId(orderId);
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setPrice(sku.getPrice());
            detail.setSkuId(sku.getId());
            detail.setTitle(sku.getTitle());
            details.add(detail);

        }
        order.setTotalPay(totalPay);
        // 实付金额 = 总金额 + 邮费 - 优惠金额
        order.setActualPay(totalPay + order.getPostFee() - 0);
        // 把order写入数据库
        int count = orderMapper.insertSelective(order);
        if (count != 1) {
            log.error("[创建订单] 创建订单失败, orderId:{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        // 把orderDetail写入数据库
        count = detailMapper.insertList(details);
        if (count != details.size()) {
            log.error("[创建订单] 创建订单详情失败, orderId:{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count = statusMapper.insert(orderStatus);
        if (count != 1) {
            log.error("[创建订单] 创建订单失败, orderId:{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 减库存
        List<CartDTO> cartDTOS = orderDTO.getCarts();
        goodsClient.decreaseStock(cartDTOS);

        return orderId;
    }

    public Order queryOrderById(Long id) {
        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        // 查询订单详情
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(id);
        List<OrderDetail> details = detailMapper.select(orderDetail);
        if (CollectionUtils.isEmpty(details)) {
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(details);
        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(id);
        if (orderStatus == null) {
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(orderStatus);
        return order;
    }

    public String createPayUrl(Long orderId) {
        // 查询订单
        Order order = queryOrderById(orderId);
        Integer status = order.getOrderStatus().getStatus();
        // 状态判断
        if (status != OrderStatusEnum.UN_PAY.value()) {
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        // 支付金额
        Long actualPay = /*order.getActualPay()*/1L;
        // 商品描述
        OrderDetail orderDetail = order.getOrderDetails().get(0);
        String title = orderDetail.getTitle();
        return payHelper.createOrder(orderId, actualPay, title);
    }

    public void handleNotify(Map<String,String> result) {
        // 数据校验
        payHelper.isSuccess(result);
        // 签名校验
        payHelper.isValidSign(result);
        // 校验金额
        String totalFeeStr = result.get("total_fee");
        String tradeNo = result.get("out_trade_no");
        if (StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)) {
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        // 获取金额
        Long totalFee = Long.valueOf(totalFeeStr);
        // 获取订单编号
        Long orderId = Long.valueOf(tradeNo);
        // 获取订单金额
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (totalFee != /*order.getActualPay()*/ 1L) {
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        // 修改订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setStatus(OrderStatusEnum.PAYED.value());
        orderStatus.setOrderId(orderId);
        orderStatus.setPaymentTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
        }
        log.info("[订单回调] 订单支付成功！订单编号:{}", orderId);
    }

    public PayState queryOrderState(Long orderId) {
        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        // 订单状态为已付款
        if (orderStatus.getStatus() != OrderStatusEnum.UN_PAY.value()) {
            return PayState.SUCCESS;
        }
        // 订单状态为未付款，去微信查询订单状态
        return payHelper.queryPayState(orderId);

    }
}
