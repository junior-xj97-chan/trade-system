package com.trade.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.R;
import com.trade.common.entity.Trade;
import com.trade.order.controller.OrderController.CreateOrderRequest;
import com.trade.order.entity.CallRecord;
import com.trade.order.entity.Order;
import com.trade.order.feign.AccountFeignClient;
import com.trade.order.feign.PositionFeignClient;
import com.trade.order.feign.ProductFeignClient;
import com.trade.order.feign.TradeFeignClient;
import com.trade.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final AccountFeignClient accountFeignClient;
    private final TradeFeignClient tradeFeignClient;
    private final PositionFeignClient positionFeignClient;
    private final ProductFeignClient productFeignClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final CallRecordService callRecordService;
    private final ObjectMapper objectMapper;
    // 幂等 Key 前缀
    private static final String IDEMPOTENT_PREFIX = "order:";
    // 幂等过期时间（分钟）
    private static final long IDEMPOTENT_EXPIRE_MINUTES = 30;

    /**
     * 创建订单（简化版，实际应配合前端页面和商品服务）
     * direction: 1=买入 2=卖出
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        // ========== 通过 Feign 查询商品信息，进行规则校验 ==========
        R<ProductFeignClient.ProductDTO> productResult = productFeignClient.getById(request.getProductId());
        if (!productResult.isSuccess() || productResult.getData() == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        ProductFeignClient.ProductDTO product = productResult.getData();

        // 停牌股票不允许买卖
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(BizCode.PRODUCT_SUSPENDED);
        }

        // A股（SH/SZ）买卖数量必须为100的整数倍
        String market = product.getMarket();
        if (("SH".equals(market) || "SZ".equals(market)) && request.getQuantity() % 100 != 0) {
            throw new BusinessException(BizCode.INVALID_QUANTITY);
        }

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setProductName(request.getProductName());
        order.setProductCode(request.getProductCode());  // 保存商品代码
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setAmount(request.getPrice().multiply(new BigDecimal(request.getQuantity())));
        // direction: 1=买入（默认）, 2=卖出
        order.setDirection(request.getDirection() != null ? request.getDirection() : 1);
        order.setSource(request.getSource() != null ? request.getSource() : 1);
        order.setStatus(1); // 待支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);
        return order;
    }

    /**
     * 支付订单
     * <p>
     * 加固后流程（先落库再调用）：
     * 1. 幂等检查 + 状态校验
     * 2. 本地事务：订单状态改为“处理中”并落库
     * 3. 本地事务提交后，按顺序发起远程调用，每次调用记录调用流水
     * 4. 远程调用成功：更新订单为“已完成”
     * 5. 远程调用失败：保留“处理中”状态，由补偿任务重试
     */
    public void payOrder(Long id) {
        // ========== 幂等性检查：防止重复支付 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "pay:" + id;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.warn("【支付订单-幂等拦截】orderId={}，请求已处理", id);
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        // 查询订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        // 已完成则直接幂等返回
        if (order.getStatus() != null && order.getStatus() == 3) {
            redisTemplate.delete(idempotentKey);
            log.info("【支付订单-幂等】orderNo={} 已完成", order.getOrderNo());
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
        }

        log.info("【支付订单】orderNo={}，金额={}", order.getOrderNo(), order.getAmount());

        // ========== 第一步：本地事务落库，状态改为“处理中” ==========
        transactionTemplate.execute(status -> {
            order.setStatus(5); // 处理中
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            return null;
        });
        log.info("【支付订单-本地落库】orderNo={} 状态已置为处理中", order.getOrderNo());

        try {
            // ========== 第二步：远程调用（本地事务已提交），带调用流水 ==========
            String bizNo = order.getOrderNo();

            // 远程调用 1：冻结余额
            Map<String, Object> freezeParams = new HashMap<>();
            freezeParams.put("userId", order.getUserId());
            freezeParams.put("amount", order.getAmount());
            freezeParams.put("orderId", id);
            callWithRecord(bizNo, "PAY", "account-service", "freezeAmount", freezeParams, () -> {
                accountFeignClient.freezeAmount(order.getUserId(), order.getAmount(), id);
                return null;
            });

            // 远程调用 2：扣减余额
            Map<String, Object> deductParams = new HashMap<>();
            deductParams.put("userId", order.getUserId());
            deductParams.put("amount", order.getAmount());
            deductParams.put("orderId", id);
            callWithRecord(bizNo, "PAY", "account-service", "deductBalance", deductParams, () -> {
                accountFeignClient.deductBalance(order.getUserId(), order.getAmount(), id);
                return null;
            });

            // 远程调用 3：创建交易记录
            Map<String, Object> tradeParams = new HashMap<>();
            tradeParams.put("orderId", id);
            tradeParams.put("userId", order.getUserId());
            tradeParams.put("productId", order.getProductId());
            tradeParams.put("price", order.getPrice());
            tradeParams.put("quantity", order.getQuantity());
            tradeParams.put("direction", 1);
            R<Trade> tradeResult = callWithRecord(bizNo, "PAY", "trade-service", "executeTrade", tradeParams, () ->
                tradeFeignClient.execute(newTradeRequest(id, order, 1))
            );
            if (tradeResult != null && !tradeResult.isSuccess()) {
                throw new BusinessException(BizCode.TRADE_NOT_FOUND.getCode(), "交易记录创建失败：" + tradeResult.getMessage());
            }

            // 远程调用 4：更新持仓
            Map<String, Object> positionParams = new HashMap<>();
            positionParams.put("orderId", id);
            positionParams.put("userId", order.getUserId());
            positionParams.put("productId", order.getProductId());
            positionParams.put("quantity", order.getQuantity());
            positionParams.put("price", order.getPrice());
            R<Void> positionResult = callWithRecord(bizNo, "PAY", "trade-service", "buyPosition", positionParams, () ->
                positionFeignClient.buy(id, order.getUserId(), order.getProductId(),
                        order.getProductName(), order.getProductCode(), order.getQuantity(), order.getPrice())
            );
            if (positionResult != null && !positionResult.isSuccess()) {
                throw new BusinessException(BizCode.POSITION_NOT_FOUND.getCode(), "持仓更新失败：" + positionResult.getMessage());
            }

            // ========== 第三步：调用成功，更新订单为“已完成” ==========
            order.setStatus(3); // 已完成
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            redisTemplate.delete(idempotentKey);
            log.info("【支付完成】orderNo={}", order.getOrderNo());

        } catch (Exception e) {
            // 远程调用失败：保留“处理中”状态，不删除幂等 key，由补偿任务继续处理
            log.error("【支付订单-远程调用失败】orderId={}，保留处理中状态等待补偿，异常={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 卖出订单
     * <p>
     * 加固后流程（先落库再调用）：
     * 1. 幂等检查 + 状态校验
     * 2. 本地事务：订单状态改为“处理中”并落库
     * 3. 本地事务提交后，按顺序发起远程调用，每次调用记录调用流水
     * 4. 调用成功：更新订单为“已完成”
     * 5. 调用失败：保留“处理中”状态，由补偿任务重试
     */
    public void sellOrder(Long id) {
        // ========== 幂等性检查：防止重复卖出 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "sell:" + id;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.warn("【卖出订单-幂等拦截】orderId={}，请求已处理", id);
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        // 查询订单
        Order order = orderMapper.selectById(id);
        if (order == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        // 已完成则直接幂等返回
        if (order.getStatus() != null && order.getStatus() == 3) {
            redisTemplate.delete(idempotentKey);
            log.info("【卖出订单-幂等】orderNo={} 已完成", order.getOrderNo());
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
        }
        if (order.getDirection() == null || order.getDirection() != 2) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ORDER_STATUS_ERROR.getCode(), "该订单不是卖出订单，无法执行卖出操作");
        }

        log.info("【卖出订单】orderNo={}，收款金额={}", order.getOrderNo(), order.getAmount());

        // ========== 第一步：本地事务落库，状态改为“处理中” ==========
        transactionTemplate.execute(status -> {
            order.setStatus(5); // 处理中
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            return null;
        });
        log.info("【卖出订单-本地落库】orderNo={} 状态已置为处理中", order.getOrderNo());

        try {
            // ========== 第二步：远程调用（本地事务已提交），带调用流水 ==========
            String bizNo = order.getOrderNo();

            // 远程调用 1：卖出收款
            Map<String, Object> receiveParams = new HashMap<>();
            receiveParams.put("userId", order.getUserId());
            receiveParams.put("amount", order.getAmount());
            receiveParams.put("orderId", id);
            callWithRecord(bizNo, "SELL", "account-service", "sellReceive", receiveParams, () -> {
                accountFeignClient.sellReceive(order.getUserId(), order.getAmount(), id);
                return null;
            });

            // 远程调用 2：创建卖出交易记录
            Map<String, Object> tradeParams = new HashMap<>();
            tradeParams.put("orderId", id);
            tradeParams.put("userId", order.getUserId());
            tradeParams.put("productId", order.getProductId());
            tradeParams.put("price", order.getPrice());
            tradeParams.put("quantity", order.getQuantity());
            tradeParams.put("direction", 2);
            R<Trade> tradeResult = callWithRecord(bizNo, "SELL", "trade-service", "executeTrade", tradeParams, () ->
                tradeFeignClient.execute(newTradeRequest(id, order, 2))
            );
            if (tradeResult != null && !tradeResult.isSuccess()) {
                throw new BusinessException(BizCode.TRADE_NOT_FOUND.getCode(), "卖出交易记录创建失败：" + tradeResult.getMessage());
            }

            // 远程调用 3：更新持仓
            Map<String, Object> positionParams = new HashMap<>();
            positionParams.put("orderId", id);
            positionParams.put("userId", order.getUserId());
            positionParams.put("productId", order.getProductId());
            positionParams.put("quantity", order.getQuantity());
            positionParams.put("price", order.getPrice());
            R<Void> positionResult = callWithRecord(bizNo, "SELL", "trade-service", "sellPosition", positionParams, () ->
                positionFeignClient.sell(id, order.getUserId(), order.getProductId(), order.getQuantity(), order.getPrice())
            );
            if (positionResult != null && !positionResult.isSuccess()) {
                throw new BusinessException(BizCode.POSITION_NOT_ENOUGH.getCode(), "持仓更新失败：" + positionResult.getMessage());
            }

            // ========== 第三步：调用成功，更新订单为“已完成” ==========
            order.setStatus(3); // 已完成
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            redisTemplate.delete(idempotentKey);
            log.info("【卖出完成】orderNo={}", order.getOrderNo());

        } catch (Exception e) {
            // 远程调用失败：保留“处理中”状态，不删除幂等 key，由补偿任务继续处理
            log.error("【卖出订单-远程调用失败】orderId={}，保留处理中状态等待补偿，异常={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 取消订单（支持退款）
     * 待支付订单：直接取消
     * 已完成订单：退款并取消（先落库再调用）
     */
    public void cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }

        Integer status = order.getStatus();
        if (status == null || status == 1) {
            // 待支付订单：直接取消
            if (order.getStatus() != null && order.getStatus() == 4) {
                log.info("【取消订单-幂等处理】orderNo={}，订单已取消", order.getOrderNo());
                return;
            }
            order.setStatus(4);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("【取消待支付订单】orderNo={}", order.getOrderNo());
        } else if (status == 3) {
            // 已完成订单：退款流程（先落库再调用）
            // ========== 幂等性检查：防止重复退款 ==========
            String idempotentKey = IDEMPOTENT_PREFIX + "cancel:" + id;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
            if (!success) {
                log.warn("【取消订单-幂等拦截】orderId={}，请求已处理", id);
                throw new BusinessException(BizCode.DUPLICATE_REQUEST);
            }

            log.info("【取消已完成订单】orderNo={}，退款金额={}", order.getOrderNo(), order.getAmount());

            // 第一步：本地事务落库，状态改为“取消处理中”
            transactionTemplate.execute(statusTx -> {
                order.setStatus(6); // 取消处理中
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                return null;
            });

            try {
                String bizNo = order.getOrderNo();

                // 远程调用 1：退款到账户
                Map<String, Object> refundParams = new HashMap<>();
                refundParams.put("userId", order.getUserId());
                refundParams.put("amount", order.getAmount());
                refundParams.put("orderId", id);
                callWithRecord(bizNo, "CANCEL", "account-service", "refund", refundParams, () -> {
                    accountFeignClient.refund(order.getUserId(), order.getAmount(), id);
                    return null;
                });

                // 远程调用 2：创建退款交易记录
                Map<String, Object> tradeParams = new HashMap<>();
                tradeParams.put("orderId", id);
                tradeParams.put("userId", order.getUserId());
                tradeParams.put("productId", order.getProductId());
                tradeParams.put("price", order.getPrice());
                tradeParams.put("quantity", order.getQuantity());
                R<Trade> tradeResult = callWithRecord(bizNo, "CANCEL", "trade-service", "refundTrade", tradeParams, () ->
                    tradeFeignClient.refund(id, order.getUserId(), order.getProductId(), order.getPrice(), order.getQuantity())
                );
                if (tradeResult != null && !tradeResult.isSuccess()) {
                    throw new BusinessException(BizCode.TRADE_NOT_FOUND.getCode(), "退款交易记录创建失败：" + tradeResult.getMessage());
                }

                // 第二步：调用成功，更新订单为“已取消”
                order.setStatus(4);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                redisTemplate.delete(idempotentKey);
                log.info("【退款完成】orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                log.error("【取消订单-远程调用失败】orderId={}，保留取消处理中状态等待补偿，异常={}", id, e.getMessage());
                throw e;
            }
        } else {
            // 其他状态（处理中、取消处理中、已取消）不允许取消
            throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
        }
    }

    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    public Order getByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    /**
     * 分页查询订单（可选按userId过滤）
     */
    public Page<Order> page(Long current, Long size, Long userId) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }

    /**
     * 查询用户的所有订单
     */
    public List<Order> getByUserId(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    /**
     * 带调用流水的远程调用封装
     * <p>
     * 1. 初始化/查询调用记录；
     * 2. 已成功的记录直接幂等跳过；
     * 3. 标记为处理中后执行调用；
     * 4. 成功/失败分别落库。
     */
    private <T> T callWithRecord(String bizNo, String bizType, String targetService, String targetMethod,
                                 Map<String, Object> params, CallAction<T> action) {
        String paramJson = toJson(params);
        CallRecord record = callRecordService.init(bizNo, bizType, targetService, targetMethod, paramJson, 5);
        if (record.getStatus() != null && record.getStatus() == 2) {
            log.info("【调用流水-已成功】bizNo={}，target={}#{}", bizNo, targetService, targetMethod);
            return null;
        }

        callRecordService.markProcessing(record.getId());
        try {
            T result = action.execute();
            callRecordService.markSuccess(record.getId(), toJson(result));
            return result;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            callRecordService.markFail(record.getId(), msg);
            throw e;
        }
    }

    /**
     * 创建交易请求对象
     */
    private TradeFeignClient.TradeRequest newTradeRequest(Long orderId, Order order, int direction) {
        TradeFeignClient.TradeRequest tradeRequest = new TradeFeignClient.TradeRequest();
        tradeRequest.setOrderId(orderId);
        tradeRequest.setUserId(order.getUserId());
        tradeRequest.setProductId(order.getProductId());
        tradeRequest.setPrice(order.getPrice());
        tradeRequest.setQuantity(order.getQuantity());
        tradeRequest.setDirection(direction);
        return tradeRequest;
    }

    /**
     * JSON 序列化工具
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败：{}", e.getMessage());
            return obj.toString();
        }
    }

    /**
     * 远程调用函数式接口
     */
    @FunctionalInterface
    private interface CallAction<T> {
        T execute();
    }
}
