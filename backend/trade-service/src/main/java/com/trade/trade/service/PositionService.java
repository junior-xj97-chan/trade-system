package com.trade.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.trade.entity.Position;
import com.trade.trade.mapper.PositionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 持仓服务
 * <p>
 * 核心业务逻辑：
 * - 买入：增加持仓数量，更新平均成本
 * - 卖出：根据持仓数量扣减
 * - 查询：按用户、按股票等多种维度查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionMapper positionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 幂等 Key 前缀
    private static final String IDEMPOTENT_PREFIX = "position:";
    // 幂等过期时间（分钟）
    private static final long IDEMPOTENT_EXPIRE_MINUTES = 30;

    // ==================== 核心持仓操作 ====================

    /**
     * 买入建仓/加仓
     * <p>
     * 计算逻辑：
     * - 如果已持有该股票（新仓）：
     *   - 持仓数量 = 本次购买数量
     *   - 平均成本 = 本次购买价格
     * - 如果已持有该股票（加仓）：
     *   - 新持仓数量 = 原数量 + 本次购买数量
     *   - 新平均成本 = (原金额 + 新金额) / 新总量
     *
     * @param orderId     订单ID（幂等性控制，一个订单只触发一次建仓/加仓）
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param productCode 商品代码（股票代码）
     * @param quantity    购买数量
     * @param price       购买价格
     */
    @Transactional(rollbackFor = Exception.class)
    public void buy(Long orderId, Long userId, Long productId, String productName, String productCode, Integer quantity, BigDecimal price) {
        // ========== 幂等性检查：一个订单只能触发一次建仓/加仓 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "buy:" + orderId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.warn("【买入持仓-幂等拦截】orderId={}，请求已处理", orderId);
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getUserId, userId)
               .eq(Position::getProductId, productId)
               .eq(Position::getStatus, 1);
        Position position = positionMapper.selectOne(wrapper);

        BigDecimal totalAmount = price.multiply(new BigDecimal(quantity));

        if (position == null) {
            // 新建持仓
            position = new Position();
            position.setUserId(userId);
            position.setProductId(productId);
            position.setProductCode(productCode);
            position.setProductName(productName);
            position.setQuantity(quantity);
            position.setAvgCost(price);
            position.setCurrentPrice(price);
            position.setStatus(1);
            position.setCreateTime(LocalDateTime.now());
            position.setUpdateTime(LocalDateTime.now());
            positionMapper.insert(position);
            log.info("【新建持仓】orderId={}, userId={}, productId={}, quantity={}, avgCost={}",
                    orderId, userId, productId, quantity, price);
        } else {
            // 加仓：使用 LambdaUpdateWrapper 绕过乐观锁（持仓记录无需版本控制）
            BigDecimal oldAmount = position.getAvgCost().multiply(new BigDecimal(position.getQuantity()));
            BigDecimal newTotalAmount = oldAmount.add(totalAmount);
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal newAvgCost = newTotalAmount.divide(new BigDecimal(newQuantity), 2, BigDecimal.ROUND_HALF_UP);

            LambdaUpdateWrapper<Position> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Position::getId, position.getId())
                         .set(Position::getQuantity, newQuantity)
                         .set(Position::getAvgCost, newAvgCost)
                         .set(Position::getCurrentPrice, price)
                         .set(Position::getProductCode, productCode)
                         .set(Position::getUpdateTime, LocalDateTime.now());
            positionMapper.update(null, updateWrapper);
            log.info("【加仓】orderId={}, userId={}, productId={}, 原有={}, 新增={}, 新总量={}, 新均价={}",
                    orderId, userId, productId, position.getQuantity(), quantity, newQuantity, newAvgCost);
        }
    }

    /**
     * 卖出减仓
     * <p>
     * 校验逻辑：
     * - 必须有足够的持仓数量
     * - 卖出后剩余数量 >= 0
     *
     * @param orderId   订单ID（幂等性控制，一个订单只能触发一次减仓）
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  卖出数量
     * @param price     卖出价格
     */
    @Transactional(rollbackFor = Exception.class)
    public void sell(Long orderId, Long userId, Long productId, Integer quantity, BigDecimal price) {
        // ========== 幂等性检查：一个订单只能触发一次减仓 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "sell:" + orderId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.warn("【卖出持仓-幂等拦截】orderId={}，请求已处理", orderId);
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getUserId, userId)
               .eq(Position::getProductId, productId)
               .eq(Position::getStatus, 1);
        Position position = positionMapper.selectOne(wrapper);

        if (position == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.POSITION_NOT_FOUND);
        }

        if (position.getQuantity() < quantity) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.POSITION_NOT_ENOUGH);
        }

        int newQuantity = position.getQuantity() - quantity;
        if (newQuantity == 0) {
            // 全部卖出，清仓：使用 LambdaUpdateWrapper 绕过乐观锁
            LambdaUpdateWrapper<Position> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Position::getId, position.getId())
                         .set(Position::getStatus, 0)
                         .set(Position::getQuantity, 0)
                         .set(Position::getCurrentPrice, price)
                         .set(Position::getUpdateTime, LocalDateTime.now());
            positionMapper.update(null, updateWrapper);
            log.info("【清仓完成】orderId={}, userId={}, productId={}", orderId, userId, productId);
        } else {
            // 部分卖出，减仓：使用 LambdaUpdateWrapper 绕过乐观锁
            LambdaUpdateWrapper<Position> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Position::getId, position.getId())
                         .set(Position::getQuantity, newQuantity)
                         .set(Position::getCurrentPrice, price)
                         .set(Position::getUpdateTime, LocalDateTime.now());
            positionMapper.update(null, updateWrapper);
            log.info("【减仓】orderId={}, userId={}, productId={}, 原有={}, 卖出={}, 剩余={}",
                    orderId, userId, productId, position.getQuantity(), quantity, newQuantity);
        }
    }

    /**
     * 更新持仓当前价格
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePrice(Long productId, BigDecimal newPrice) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getProductId, productId)
               .eq(Position::getStatus, 1);
        List<Position> positions = positionMapper.selectList(wrapper);

        for (Position position : positions) {
            position.setCurrentPrice(newPrice);
            position.setUpdateTime(LocalDateTime.now());
            positionMapper.updateById(position);
        }
        log.info("【更新持仓价格】productId={}, newPrice={}, affected={}", productId, newPrice, positions.size());
    }

    // ==================== 查询操作 ====================

    /**
     * 查询用户的所有持仓
     */
    public List<Position> getByUserId(Long userId) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getUserId, userId)
               .eq(Position::getStatus, 1)
               .orderByDesc(Position::getUpdateTime);
        List<Position> positions = positionMapper.selectList(wrapper);
        // 计算浮动盈亏
        for (Position p : positions) {
            p.calculateProfitLoss();
        }
        return positions;
    }

    /**
     * 查询用户的单个持仓
     */
    public Position getByUserIdAndProductId(Long userId, Long productId) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getUserId, userId)
               .eq(Position::getProductId, productId)
               .eq(Position::getStatus, 1);
        Position position = positionMapper.selectOne(wrapper);
        if (position != null) {
            position.calculateProfitLoss();
        }
        return position;
    }

    /**
     * 分页查询所有持仓
     */
    public Page<Position> page(Long current, Long size) {
        Page<Position> page = new Page<>(current, size);
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getStatus, 1)
               .orderByDesc(Position::getUpdateTime);
        Page<Position> result = positionMapper.selectPage(page, wrapper);
        // 计算浮动盈亏
        for (Position p : result.getRecords()) {
            p.calculateProfitLoss();
        }
        return result;
    }

    public Position getById(Long id) {
        Position position = positionMapper.selectById(id);
        if (position != null) {
            position.calculateProfitLoss();
        }
        return position;
    }
}
