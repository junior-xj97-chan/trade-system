package com.trade.search.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ES 商品文档实体
 */
@Document(indexName = "product_index")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品文档实体")
public class ProductDocument {

    @Id
    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品代码（如 600519.SH）", example = "600519.SH")
    @Field(type = FieldType.Keyword)
    private String productCode;

    @Schema(description = "商品名称（支持全文搜索）", example = "贵州茅台")
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    @Schema(description = "商品类型（stock-股票、fund-基金、bond-债券、future-期货）", example = "stock")
    @Field(type = FieldType.Keyword)
    private String productType;

    @Schema(description = "交易所代码（SH、SZ、HK、US）", example = "SH")
    @Field(type = FieldType.Keyword)
    private String exchangeCode;

    @Schema(description = "当前价格", example = "1688.00")
    @Field(type = FieldType.Double)
    private BigDecimal currentPrice;

    @Schema(description = "昨收价格", example = "1700.00")
    @Field(type = FieldType.Double)
    private BigDecimal yesterdayClose;

    @Schema(description = "今日开盘价", example = "1690.00")
    @Field(type = FieldType.Double)
    private BigDecimal todayOpen;

    @Schema(description = "最高价", example = "1700.00")
    @Field(type = FieldType.Double)
    private BigDecimal highPrice;

    @Schema(description = "最低价", example = "1680.00")
    @Field(type = FieldType.Double)
    private BigDecimal lowPrice;

    @Schema(description = "成交量", example = "1000000")
    @Field(type = FieldType.Long)
    private Long volume;

    @Schema(description = "成交额", example = "1688000000.00")
    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Schema(description = "涨跌额", example = "-12.00")
    @Field(type = FieldType.Double)
    private BigDecimal changeAmount;

    @Schema(description = "涨跌幅（百分比）", example = "-0.71")
    @Field(type = FieldType.Double)
    private BigDecimal changePercent;

    @Schema(description = "市值", example = "2120000000000.00")
    @Field(type = FieldType.Double)
    private BigDecimal marketValue;

    @Schema(description = "流通市值", example = "2120000000000.00")
    @Field(type = FieldType.Double)
    private BigDecimal circulateMarketValue;

    @Schema(description = "换手率", example = "0.35")
    @Field(type = FieldType.Double)
    private BigDecimal turnoverRate;

    @Schema(description = "市盈率", example = "35.50")
    @Field(type = FieldType.Double)
    private BigDecimal pe;

    @Schema(description = "市净率", example = "12.80")
    @Field(type = FieldType.Double)
    private BigDecimal pb;

    @Schema(description = "简介/描述")
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Schema(description = "商品状态（1-正常、0-停牌、-1-退市）", example = "1")
    @Field(type = FieldType.Integer)
    private Integer status;

    @Schema(description = "更新时间")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updateTime;

    @Schema(description = "创建时间")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createTime;
}
