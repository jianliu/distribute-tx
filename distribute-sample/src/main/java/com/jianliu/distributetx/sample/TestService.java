package com.jianliu.distributetx.sample;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.anno.DTMethod;
import com.jianliu.distributetx.anno.DTransactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * distribute-tx
 *
 * @author jian.liu
 * @since 2019/6/19
 */
@Service
public class TestService extends BaseLogger {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @DTransactional
    public void doTransaction(Integer i){

        String sql = "INSERT INTO `test` (`name`) " +
                "VALUES(?)";
        int result = jdbcTemplate.update(sql, "oh yeah");
        if (result == 1) {
            logger.info("save ok");
        }

        unTransactionUpdate(i);
    }


    @DTMethod
    public void unTransactionUpdate(Integer i){
        logger.info("unTransactionUpdate run:" + i);
    }

}
