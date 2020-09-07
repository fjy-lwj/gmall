package com.atguigu.gmall.common.exception;

import lombok.Data;

/**
 * 简易的 统一异常处理
 */

@Data
public class OrderException extends RuntimeException {
    public OrderException() {
    }

    public OrderException(String message) {
        super(message);
    }
}
