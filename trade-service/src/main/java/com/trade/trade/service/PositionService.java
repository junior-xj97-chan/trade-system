package com.trade.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param price       购买价格
     */
    @Transactional(rollbackFor = Exception.class)
    public void buy(Long userId, Long productId, String productName, Integer quantity, BigDecimal price) {
        // ========== 幂等性检查：防止重复建仓/加仓 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "buy:" + userId + ":" + productId + ":" + quantity;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
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
            position.setProductName(productName);
            position.setQuantity(quantity);
            position.setAvgCost(price);
            position.setCurrentPrice(price);
            position.setStatus(1);
            position.setCreateTime(LocalDateTime.now());
            position.setUpdateTime(LocalDateTime.now());
            positionMapper.insert(position);
            log.info("【新建持仓】userId={}, productId={}, quantity={}, avgCost={}",
                    userId, productId, quantity, price);
        } else {
            // 加仓
            BigDecimal oldAmount = position.getAvgCost().multiply(new BigDecimal(position.getQuantity()));
            BigDecimal newTotalAmount = oldAmount.add(totalAmount);
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal newAvgCost = newTotalAmount.divide(new BigDecimal(newQuantity), 2, BigDecimal.ROUND_HALF_UP);

            position.setQuantity(newQuantity);
            position.setAvgCost(newAvgCost);
            position.setCurrentPrice(price);
            position.setUpdateTime(LocalDateTime.now());
            positionMapper.updateById(position);
            log.info("【加仓】userId={}, productId={}, 原有={}, 新增={}, 新总量={}, 新均价={}",
                    userId, productId, position.getQuantity() - quantity, quantity, newQuantity, newAvgCost);
        }
    }

    /**
     * 卖出减仓
     * <p>
     * 校验逻辑：
     * - 必须有足够的持仓数量
     * - 卖出后剩余数量 >= 0
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  卖出数量
     * @param price     卖出价格
     */
    @Transactional(rollbackFor = Exception.class)
    public void sell(Long userId, Long productId, Integer quantity, BigDecimal price) {
        // ========== 幂等性检查：防止重复减仓 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "sell:" + userId + ":" + productId + ":" + quantity;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
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
            // 全部卖出，清仓
            position.setStatus(0); // 已清仓
            position.setQuantity(0);
            position.setCurrentPrice(price);
            position.setUpdateTime(LocalDateTime.now());
            positionMapper.updateById(position);
            log.info("【清仓完成】userId={}, productId={}", userId, productId);
        } else {
            // 部分卖出，减仓
            position.setQuantity(newQuantity);
            position.setCurrentPrice(price);
            position.setUpdateTime(LocalDateTime.now());
            positionMapper.updateById(position);
            log.info("【减仓】userId={}, productId={}, 原有={}, 卖出={}, 剩余={}",
                    userId, productId, position.getQuantity() + quantity, quantity, newQuantity);
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
        return positionMapper.selectList(wrapper);
    }

    /**
     * 查询用户的单个持仓
     */
    public Position getByUserIdAndProductId(Long userId, Long productId) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getUserId, userId)
               .eq(Position::getProductId, productId)
               .eq(Position::getStatus, 1);
        return positionMapper.selectOne(wrapper);
    }

    /**
     * 分页查询所有持仓
     */
    public Page<Position> page(Long current, Long size) {
        Page<Position> page = new Page<>(current, size);
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getStatus, 1)
               .orderByDesc(Position::getUpdateTime);
        return positionMapper.selectPage(page, wrapper);
    }

    public Position getById(Long id) {
        return positionMapper.selectById(id);
    }
}
