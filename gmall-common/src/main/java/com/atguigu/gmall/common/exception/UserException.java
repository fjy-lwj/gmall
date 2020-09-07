package com.atguigu.gmall.common.exception;

import lombok.Data;

/**
 * 简易的 统一异常处理
 */
@Data
public class UserException extends RuntimeException {
    public UserException() {
    }

    public UserException(String message) {
        super(message);
    }
}
