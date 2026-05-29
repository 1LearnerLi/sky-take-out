package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO sumbitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1. 处理业务逻辑异常
        //判断地址簿数据是否为空
        //获取地址簿id
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        //查询地址簿数据
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        //判断是否为空
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //检查用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        //判断购物车数据是否为空
        //获取用户Id
        Long userId = BaseContext.getCurrentId();
        //查询该用户的购物车
        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.selectList(wrapper);
        //判断是否为空
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2. 订单表数据插入
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setAddressBookId(addressBookId);
        order.setPayStatus(Orders.UN_PAID);
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        order.setAddress(addressBook.getDetail());
        //我是用的是自动填充，但最后发现只有一张表中有order_time字段，还不如直接插入更方便更快。多张数据表都有同一字段：如create_time时，才适合用自动填充
//        order.setOrderTime(LocalDateTime.now());

        orderMapper.insert(order);

        //3. 订单明细表插入数据
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (ShoppingCart shoppingCart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insert(orderDetailList);

        //4. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //5. 返回OrderSubmitVO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .orderTime(order.getOrderTime())
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .build();


        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        /*if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/
        //查询该订单是否已支付
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getNumber, ordersPaymentDTO.getOrderNumber());
        Orders order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer payStatus = order.getPayStatus();
        if (payStatus == Orders.PAID) {
            throw new OrderBusinessException("该订单已支付");
        }



        /*OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));*/

        return null;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过websocket向客户端浏览器发送消息 type orderId content
        Map map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);


    }

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        //OrderVO类继承了Order，所以OrderVO有Order全部属性
        //使用OrderVO的集合作为返回值
        List<OrderVO> orderVOS = new ArrayList<>();
        //分页查询所有订单
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, BaseContext.getCurrentId())
                .eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus());
        orderMapper.selectPage(page, wrapper);
        List<Orders> records = page.getRecords();
        //判断结果是否为空
        if (records != null && records.size() > 0) {
            //遍历每份订单，并将其拷贝进orderVO，并为orderVO的DetailList属性赋值
            for (Orders record : records) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(record, orderVO);
                //查询每份订单的订单明细表
                Long orderId = record.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
                //为orderVO的DetailList属性赋值
                orderVO.setOrderDetailList(orderDetails);
                //将每份orderVO封装进orderVOS集合中
                orderVOS.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), orderVOS);
    }

    /**
     * 查询订单详情
     *
     * @param orderId
     * @return
     */
    public OrderVO orderDetail(String orderId) {
        Orders order = orderMapper.selectById(orderId);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param orderId
     */
    public void cancleOrder(String orderId) {
        //查看订单状态
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer orderStatus = order.getStatus();
        //商家已接单状态与派送中状态下，用户取消订单需电话沟通商家，所以订单状态在待接单之后的话就抛异常
        if (orderStatus > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //待付款和待接单状态下，可直接取消订单
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户取消");
        //如果在待接单状态下取消订单，需要给用户退款
        if (orderStatus == Orders.TO_BE_CONFIRMED) {
            order.setPayStatus(Orders.REFUND);
        }
        orderMapper.updateById(order);
    }

    /**
     * 再来一单
     *
     * @param orderId
     */
    public void oneMoreOrder(String orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order != null) {
            //根据orderId查询订单明细表
            List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
            //将订单明细表数据添加到购物车
            List<ShoppingCart> shoppingCarts = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetails) {
                ShoppingCart cart = new ShoppingCart();
                BeanUtils.copyProperties(orderDetail, cart);
                cart.setUserId(BaseContext.getCurrentId());
                shoppingCarts.add(cart);
            }
            //插入购物车数据
            shoppingCartMapper.insert(shoppingCarts);
        }
    }

    /**
     * 管理端订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult search(OrdersPageQueryDTO ordersPageQueryDTO) {
        //分页查找所有订单
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        orderMapper.conditionSearch(page, ordersPageQueryDTO);

        //将每个订单数据order封装进orderVO
        List<OrderVO> orderVOS = new ArrayList<>();
        List<Orders> records = page.getRecords();
        if (records != null && records.size() > 0) {
            for (Orders order : records) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                //查询出指定orderId的orderDetail数据，拼成字符串，封装进orderVO
                Long orderId = order.getId();
                orderVO.setOrderDishes(getOrderDishes(orderId));
                //将所有orderVO封装进orderVOS集合
                orderVOS.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), orderVOS);
    }

    private String getOrderDishes(Long orderId) {
        //查询出指定orderId的orderDetail数据
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        String orderDishes = "";
        for (OrderDetail orderDetail : orderDetails) {
            //将名字价格拼成字符串
            String name = orderDetail.getName() + "*" + orderDetail.getNumber() + ";" + " ";
            orderDishes += name;
        }

        /*//另一种方式： 使用stream.map生成List新集合，之后使用String.join将List转成String
        List<String> list = orderDetails.stream().map(x -> {
            String name = x.getName() + "*" + x.getNumber() + ";";
            return name;
        }).collect(Collectors.toList());
        return String.join(" ",list);*/

        return orderDishes;
    }

    /**
     * 管理端各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        //待派送
        LambdaQueryWrapper<Orders> confirmedWrapper = new LambdaQueryWrapper<>();
        //派送中
        LambdaQueryWrapper<Orders> deliveryInProgressWrapper = new LambdaQueryWrapper<>();
        //待接单
        LambdaQueryWrapper<Orders> toBeConfirmedWrapper = new LambdaQueryWrapper<>();

        //查询待派送数量
        confirmedWrapper.eq(Orders::getStatus, Orders.CONFIRMED);
        Long confirmed = orderMapper.selectCount(confirmedWrapper);

        //查询派送中数量
        deliveryInProgressWrapper.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        Long deliveryInProgress = orderMapper.selectCount(deliveryInProgressWrapper);

        //查询待接单数量
        toBeConfirmedWrapper.eq(Orders::getStatus, Orders.TO_BE_CONFIRMED);
        Long toBeConfirmed = orderMapper.selectCount(toBeConfirmedWrapper);

        //封装进OrderStatisticsVO
        return new OrderStatisticsVO(Math.toIntExact(toBeConfirmed), Math.toIntExact(confirmed), Math.toIntExact(deliveryInProgress));
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = orderMapper.selectById(ordersConfirmDTO.getId());
        if (order != null) {
            order.setStatus(Orders.CONFIRMED);
        }
        orderMapper.updateById(order);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejectionOrder(OrdersRejectionDTO ordersRejectionDTO) {
        //查看订单状态，只有订单处于“待接单”状态时才可以执行拒单操作
        Orders order = orderMapper.selectById(ordersRejectionDTO.getId());
        if (order == null || order.getStatus() != Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //判断是否已付款
        Integer payStatus = order.getPayStatus();
        //如果已支付
        if (payStatus == Orders.PAID) {
            order.setPayStatus(Orders.REFUND);
        }

        order.setStatus(Orders.CANCELLED);
        order.setCancelReason(ordersRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);

    }

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    public void admincancleOrder(OrdersCancelDTO ordersCancelDTO) {
        //查看订单状态
        Long orderId = ordersCancelDTO.getId();
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer orderStatus = order.getStatus();
        //只有订单处于“派送中”状态之前时才可以执行取消操作
        if (orderStatus > 3) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(ordersCancelDTO.getCancelReason());
        //如果用户已支付，则退款
        if (order.getPayStatus() == Orders.PAID) {
            order.setPayStatus(Orders.REFUND);
        }
        orderMapper.updateById(order);

    }

    /**
     * 派送订单
     *
     * @param orderId
     */
    public void deliveryOrder(String orderId) {
        //查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //查询订单状态，只有订单处于“已接单（待派送）”状态之时才可以执行派送操作
        Integer orderStatus = order.getStatus();
        if (orderStatus != Orders.CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //设置状态为派送中
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.updateById(order);
    }

    /**
     * 完成订单
     *
     * @param orderId
     */
    public void completeOrder(String orderId) {
        //查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //查询订单状态，只有订单处于“4派送中”状态之时才可以执行完成操作
        Integer orderStatus = order.getStatus();
        if (orderStatus != Orders.DELIVERY_IN_PROGRESS) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //设置状态为已完成
        order.setStatus(Orders.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 客户催单
     *
     * @param orderId
     */
    public void reminderOrder(Long orderId) {
        //查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap<>();
        map.put("type",2);
        map.put("orderId", orderId);
        map.put("content","订单号：" +order.getNumber());

        //通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     *
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address", address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
