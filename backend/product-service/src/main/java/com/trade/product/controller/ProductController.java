package com.trade.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.trade.common.R;
import com.trade.common.entity.ProductDTO;
import com.trade.product.entity.Product;
import com.trade.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "商品管理")
public class ProductController {

    private final ProductService productService;

    /**
     * 添加商品
     */
    @PostMapping("/add")
    @Operation(summary = "添加商品")
    public R<Product> add(@RequestBody Product product) {
        if (product == null || !StringUtils.hasText(product.getProductCode())) {
            return R.fail("商品代码不能为空");
        }
        // 检查商品代码是否已存在
        Product exist = productService.getByProductCode(product.getProductCode());
        if (exist != null) {
            return R.fail("商品代码已存在");
        }
        boolean success = productService.save(product);
        return success ? R.ok(product) : R.fail("添加商品失败");
    }

    /**
     * 删除商品（逻辑删除）
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "删除商品（逻辑删除）")
    public R<Void> delete(@PathVariable Long productId) {
        if (productId == null) {
            return R.fail("商品ID不能为空");
        }
        boolean success = productService.removeById(productId);
        return success ? R.ok() : R.fail("删除商品失败");
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新商品信息")
    public R<Void> update(@RequestBody Product product) {
        if (product == null || product.getId() == null) {
            return R.fail("商品ID不能为空");
        }
        boolean success = productService.updateById(product);
        return success ? R.ok() : R.fail("更新商品失败");
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{productId}")
    @Operation(summary = "查询商品详情")
    public R<ProductDTO> detail(@PathVariable Long productId) {
        if (productId == null) {
            return R.fail("商品ID不能为空");
        }
        Product product = productService.getById(productId);
        if (product == null) {
            return R.fail("商品不存在");
        }
        return R.ok(convertToDTO(product));
    }

    /**
     * 根据商品代码查询
     */
    @SentinelResource("product:getByCode")
    @GetMapping("/code/{productCode}")
    @Operation(summary = "根据商品代码查询")
    public R<ProductDTO> getByCode(@PathVariable String productCode) {
        Product product = productService.getByProductCode(productCode);
        if (product == null) {
            return R.fail("商品不存在");
        }
        return R.ok(convertToDTO(product));
    }

    /**
     * 实体转换为 DTO
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setProductName(product.getProductName());
        dto.setCurrentPrice(product.getCurrentPrice());
        dto.setCategory(product.getCategory());
        dto.setStatus(product.getStatus());
        dto.setCreateTime(product.getCreateTime());
        dto.setUpdateTime(product.getUpdateTime());
        return dto;
    }

    /**
     * 分页查询商品列表
     */
    @SentinelResource("product:page")
    @GetMapping("/page")
    @Operation(summary = "分页查询商品列表")
    public R<Page<Product>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "商品分类") @RequestParam(required = false) Integer category,
            @Parameter(description = "商品状态") @RequestParam(required = false) Integer status) {
        Page<Product> page = new Page<>(current, size);
        Page<Product> result = productService.pageList(page, keyword, category, status);
        return R.ok(result);
    }

    /**
     * 更新商品价格
     */
    @PostMapping("/updatePrice")
    @Operation(summary = "更新商品价格")
    public R<Void> updatePrice(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "新价格") @RequestParam BigDecimal newPrice) {
        boolean success = productService.updatePrice(productId, newPrice);
        return success ? R.ok() : R.fail("更新价格失败");
    }

    /**
     * 上架商品
     */
    @PostMapping("/online/{productId}")
    @Operation(summary = "上架商品")
    public R<Void> online(@PathVariable Long productId) {
        boolean success = productService.online(productId);
        return success ? R.ok() : R.fail("上架失败");
    }

    /**
     * 下架商品
     */
    @PostMapping("/offline/{productId}")
    @Operation(summary = "下架商品")
    public R<Void> offline(@PathVariable Long productId) {
        boolean success = productService.offline(productId);
        return success ? R.ok() : R.fail("下架失败");
    }
}
