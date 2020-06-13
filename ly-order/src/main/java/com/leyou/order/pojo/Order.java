package com.leyou.order.pojo;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@Data
@Table(name = "tb_order")
public class Order {
    @Id
    private Long orderId;
    private Long totalPay;
    private Long actualPay;
    private Integer paymentType; // 支付类型
    private String promotionIds; // 参与促销活动的id
    private Long postFee = 0L; // 邮费
    private Date createTime;
    private String shippingName; // 物流名称
    private String shippingCode; // 物流编号
    private Long userId;
    private String buyerMessage; // 买家留言
    private String buyerNick; // 买家昵称
    private Boolean buyerRate; // 卖家是否已评价
    private String receiver; // 收货人全名
    private String receiverMobile;
    private String receiverState;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverAddress;
    private String receiverZip; // 邮政编码
    private Integer invoiceType = 0; // 发票类型，0：无发票，1：普通发票，2：电子发票，3：增值税发票
    private Integer sourceType = 1; // 订单来源，1：app端，2：pc端，3：M端，4：微信端，5：手机qq端

    @Transient
    private OrderStatus orderStatus; // 订单状态
    @Transient
    private List<OrderDetail> orderDetails; // 订单详情
}
