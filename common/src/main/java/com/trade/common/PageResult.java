package com.trade.common;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装
 */
@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前页 */
    private Long current;
    
    /** 每页大小 */
    private Long size;
    
    /** 总记录数 */
    private Long total;
    
    /** 总页数 */
    private Long pages;
    
    /** 数据列表 */
    private List<T> records;

    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages((total + size - 1) / size);
        return result;
    }

    public static <T> PageResult<T> empty(Long current, Long size) {
        return of(List.of(), 0L, current, size);
    }
}
