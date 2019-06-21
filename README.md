# distribute-tx
分布式事务，保证mysql主事务业务与非事务业务的最终一致性

##典型场景

    begin transaction
    mysql.update 
    redis.delete
    mongodb.update
    commit or rollback
    
正常情况下可能会出现多种数据不一致的情况

情况1
 
    mongodb.update    成功
    redis.delete 成功
    mysql.update 失败
    transaction rollback
    => mongodb更新的数据和mysql的数据不一致

     
情况2
  
    mysql.update 成功
    redis.delete 成功
    mongodb.update 成功
    =>te
    另一个线在redis.delete后，transaction commit前 立即查询mysql，可能出现查到老数据的情况
    
##解决方案
将非事务操作封装成task记录和mysql更新一起写入同一个mysql库，利用单库的ACID来保证事务一致性
异步任务获取task任务后找到对应的bean执行对应的方法，达到最终一致性的效果

如果尝试5次仍然执行失败，提供    FailureJobHandler 机制，允许人工介入失败任务

利用一致性hash思路来保证每台机器获取的任务互不干扰，正常情况下每台机器仅会获取自己生产的任务
如果机器A down掉后，其他机器可自动接手A的任务，防止任务被长时间延迟执行

##使用方式

### MAVEN依赖及插件引入

        <dependency>
            <groupId>com.jianliu</groupId>
            <artifactId>distribute-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
        <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>aspectj-maven-plugin</artifactId>
                <version>1.10</version>
                <configuration>
                    <complianceLevel>1.8</complianceLevel>
                    <source>1.8</source>
                    <target>1.8</target>
                    <showWeaveInfo>true</showWeaveInfo>
                    <!--<verbose>true</verbose>-->
                    <!--<encoding>utf-8</encoding>-->
                    <aspectLibraries>
                        <aspectLibrary>
                            <groupId>com.jianliu</groupId>
                            <artifactId>distribute-core</artifactId>
                        </aspectLibrary>
                    </aspectLibraries>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <!--use this goal to weave all your main classes-->
                            <goal>compile</goal>
                            <!--use this goal to weave all your test classes-->
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>        

###代码引入

配置property

    distribute.tx.appName=yesApp
    distribute.tx.serializer=com.jianliu.distributetx.serde.JacksonSerializer

Spring配置

    扫描路径或加载
    com.jianliu.distributetx.config.DistributeTxConfiguration   
    将业务DataSource的id设置为distributeTxDatasource，Router的DataSource或普通DataSource皆可
    如果全局只有一个Datasource，id不强制为distributeTxDatasource
    
代码引入

    @DTransactional //使用DTransactional注解声明事务，该注解继承Spring的Transactional注解效果
    public void doTransaction(Integer i){

        String sql = "INSERT INTO `test` (`name`) " +
                "VALUES(?)";
        int result = jdbcTemplate.update(sql, "oh yeah");
        if (result == 1) {
            logger.info("save ok");
        }

        unTransactionUpdate(i);
    }


    @DTMethod //声明非事务任务方法，被该注解声明后，会将方法名称、参数写入mysql task记录表，异步执行
    public void unTransactionUpdate(Integer i){
        logger.info("unTransactionUpdate run:" + i);
    }    

###运行前

执行mvn package打包编译，使aspectj可重新编译带@DTransactional、 @DTMethod 的Class，达到分布式事务增强效果
Aspectj本身无自调用问题，请勿担心    

###注意点

由于aspectj对版本依赖较为严重，使用时可能需要根据当前jdk版本来调整aspectj版本及插件版本
同时aspectj目前暂时无法和lombok共存