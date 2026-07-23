package com.trade.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trade.product.entity.Product;
import com.trade.product.mapper.ProductMapper;
import com.trade.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY_PREFIX = "trade:product:";

    @Override
    public Product getByProductCode(String productCode) {
        if (!StringUtils.hasText(productCode)) {
            return null;
        }
        // 先从 Redis 缓存中获取
        String redisKey = REDIS_KEY_PREFIX + productCode;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null && cached instanceof Product) {
            log.debug("【商品缓存命中】productCode={}", productCode);
            return (Product) cached;
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0);
        Product product = productMapper.selectOne(wrapper);
        if (product != null) {
            // 存入 Redis，有效期 5 分钟
            redisTemplate.opsForValue().set(redisKey, product, Duration.ofMinutes(5));
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Product product) {
        boolean success = super.save(product);
        if (success) {
            log.info("【商品新增】productId={}, productCode={}", product.getId(), product.getProductCode());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Product product) {
        boolean success = super.updateById(product);
        if (success) {
            // 清除缓存
            String redisKey = REDIS_KEY_PREFIX + product.getProductCode();
            redisTemplate.delete(redisKey);
            log.info("【商品修改】productId={}, productCode={}", product.getId(), product.getProductCode());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return false;
        }
        boolean success = super.removeById(productId);
        if (success) {
            // 清除缓存
            String redisKey = REDIS_KEY_PREFIX + product.getProductCode();
            redisTemplate.delete(redisKey);
            log.info("【商品删除】productId={}, productCode={}", productId, product.getProductCode());
        }
        return success;
    }

    @Override
    public boolean updatePrice(Long productId, BigDecimal newPrice) {
        if (productId == null || newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            return false;
        }
        product.setCurrentPrice(newPrice);
        int rows = productMapper.updateById(product);
        if (rows > 0) {
            // 更新缓存
            String redisKey = REDIS_KEY_PREFIX + product.getProductCode();
            redisTemplate.delete(redisKey);
            log.info("【商品价格更新】productId={}, newPrice={}", productId, newPrice);
            return true;
        }
        return false;
    }

    @Override
    public Page<Product> pageList(Page<Product> page, String keyword, Integer category, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getProductCode, keyword)
                    .or().like(Product::getProductName, keyword));
        }
        if (category != null) {
            wrapper.eq(Product::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean online(Long productId) {
        return updateStatus(productId, 1);
    }

    @Override
    public boolean offline(Long productId) {
        return updateStatus(productId, 0);
    }

    private boolean updateStatus(Long productId, int targetStatus) {
        if (productId == null) {
            return false;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            return false;
        }
        if (Objects.equals(product.getStatus(), targetStatus)) {
            return true;
        }
        product.setStatus(targetStatus);
        int rows = productMapper.updateById(product);
        if (rows > 0) {
            // 清除缓存
            String redisKey = REDIS_KEY_PREFIX + product.getProductCode();
            redisTemplate.delete(redisKey);
            log.info("【商品状态更新】productId={}, status={}", productId, targetStatus);
            return true;
        }
        return false;
    }

    /**
     * 从商品代码提取交易所代码
     */
    private String extractExchange(String productCode) {
        if (productCode == null || productCode.isEmpty()) {
            return "OTHER";
        }
        if (productCode.endsWith(".SH") || productCode.endsWith(".SZ")) {
            return productCode.substring(productCode.length() - 2);
        }
        if (productCode.startsWith("6")) {
            return "SH";
        } else if (productCode.startsWith("0") || productCode.startsWith("3")) {
            return "SZ";
        }
        return "OTHER";
    }
}
