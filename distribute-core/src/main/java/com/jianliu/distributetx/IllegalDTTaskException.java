package com.jianliu.distributetx;

/**
 * distribute-tx
 *
 * @author jian.liu
 * @since 2019/6/22
 */
public class IllegalDTTaskException extends RuntimeException{

    public IllegalDTTaskException() {
    }

    public IllegalDTTaskException(String message) {
        super(message);
    }

    public IllegalDTTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDTTaskException(Throwable cause) {
        super(cause);
    }

    public IllegalDTTaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
