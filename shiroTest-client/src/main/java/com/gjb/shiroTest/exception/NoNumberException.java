package com.gjb.shiroTest.exception;

/**
 * @author gejinbiao@ucfgroup.com
 * @Title
 * @Description:
 * @Company: ucfgroup.com
 * @Created 2018-01-04
 */
/**
 * 库存不足异常
 */
public class NoNumberException extends RuntimeException {

    public NoNumberException(String message) {
        super(message);
    }

    public NoNumberException(String message, Throwable cause) {
        super(message, cause);
    }

}
