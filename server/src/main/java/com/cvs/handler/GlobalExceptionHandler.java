package com.cvs.handler;

import com.cvs.constant.MessageConstant;
import com.cvs.exception.BaseException;
import com.cvs.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理SQL异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result sqlExceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        log.error("异常信息：{}",message);
        if (message.contains("Duplicate entry")){
            String[] error = message.split(" ");
            String username = error[2];
            String msg = username.substring(1,username.length()-1) + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else return Result.error(MessageConstant.UNKNOWN_ERROR);

    }

}
