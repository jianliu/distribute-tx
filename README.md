# distribute-tx
分布式事务，保证mysql主事务业务与非事务业务的最终一致性

## 典型场景

    begin transaction
    mysql.update 
    mongodb.update
    redis.delete
    ... 操作 可能异常 ...
    commit or rollback
    
正常情况下可能会出现多种数据不一致的情况

情况1
 
    mongodb.update    成功 //mongodb已修改
    mysql.update 失败
    redis.delete ..  //异常导致rollback，redis.delete不会执行
    transaction rollback
    => mongodb更新的数据和mysql的数据不一致

     
情况2
  
    mongodb.update    成功
    mysql.update 成功
    redis.delete 成功
    mq.send()
    ...
    transaction commit
    =>te
    另一个线程在redis.delete后,transaction commit前 立即查询mysql并缓存，可能出现缓存老数据的情况
    如果在事务中直接设置redis缓存，又可能出现情况1遭遇的难题

情况2是一个更加接近现实场景的例子,由于mysql.update是成功的，因此redis.delete执行，此时由于mysql事务未提交，则其他线程发现缓存不存在时，可能立即更缓存，而此时mysql库中的数据仍然是未更新的，这直接导致redis缓存没有刷新，其他的情况如查询时的读写分离将进一步加剧其概率。
同样的，假设redis.delete或其后的代码执行失败，将导致事务回滚，但这个回滚原因似乎不具备很强的说服力：一个非主业务的缓存删除失败，并且有可能是一个网络抖动，导致上方一个非常复杂的sql序列执行功亏一篑，这不是我们真正想见到的
    
## 解决方案
将非事务操作封装成task记录和mysql更新一起写入同一个mysql库，利用单库的ACID来保证事务一致性
异步任务获取task任务后找到对应的bean执行对应的方法，达到最终一致性的效果

如果尝试5次仍然执行失败，提供    FailureJobHandler 机制，允许人工介入失败任务

利用一致性hash思路来保证每台机器获取的任务互不干扰，正常情况下每台机器仅会获取自己生产的任务
如果机器A down掉后，其他机器可自动接手A的任务，防止任务被长时间延迟执行

## 使用方式

使用此框架，要求非事务任务方法必须是幂等的，即多次执行效果相同，重复执行不会造成数据混乱
如自定义业务编号作为方法入参

### MAVEN依赖及插件引入

       <properties>
                <java.source-target.version>1.8</java.source-target.version>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                <aspectj.version>1.9.2</aspectj.version>
        </properties>

        <dependency>
            <groupId>com.jianliu</groupId>
            <artifactId>distribute-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
       
        
        <plugin>
                <!-- 指定maven编译的jdk版本,如果不指定,maven3默认用jdk 1.5 -->
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.1</version>
                <configuration>
                    <!-- 一般而言，target与source是保持一致的，但是，有时候为了让程序能在其他版本的jdk中运行(对于低版本目标jdk，源代码中不能使用低版本jdk中不支持的语法)，会存在target不同于source的情况 -->
                    <source>${java.source-target.version}</source> <!-- 源代码使用的JDK版本 -->
                    <target>${java.source-target.version}</target> <!-- 需要生成的目标class文件的编译版本 -->
                    <encoding>${project.build.sourceEncoding}</encoding><!-- 字符集编码 -->
                    <!--<skipTests>true</skipTests>&lt;!&ndash; 跳过测试 &ndash;&gt;-->
                    <!--<verbose>true</verbose>-->
                    <!--<showWarnings>true</showWarnings>-->
                    <!--<fork>true</fork>&lt;!&ndash; 要使compilerVersion标签生效，还需要将fork设为true，用于明确表示编译版本配置的可用 &ndash;&gt;-->
                    <!--<executable>&lt;!&ndash; path-to-javac &ndash;&gt;</executable>&lt;!&ndash; 使用指定的javac命令，例如：<executable>${JAVA_1_4_HOME}/bin/javac</executable> &ndash;&gt;-->
                    <!--<compilerVersion>1.3</compilerVersion>&lt;!&ndash; 指定插件将使用的编译器的版本 &ndash;&gt;-->
                    <!--<meminitial>128m</meminitial>&lt;!&ndash; 编译器使用的初始内存 &ndash;&gt;-->
                    <!--<maxmem>512m</maxmem>&lt;!&ndash; 编译器使用的最大内存 &ndash;&gt;-->
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>aspectj-maven-plugin</artifactId>
                <version>1.10</version>
                <configuration>
                    <!--<showWeaveInfo>true</showWeaveInfo> -->
                    <source>${java.source-target.version}</source>
                    <target>${java.source-target.version}</target>
                    <Xlint>ignore</Xlint>
                    <complianceLevel>${java.source-target.version}</complianceLevel>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <!--<verbose>true</verbose> -->
                    <!--<warn>constructorName,packageDefaultMethod,deprecation,maskedCatchBlocks,unusedLocals,unusedArguments,unusedImport</warn> -->
                    <aspectLibraries>
                        <aspectLibrary>   //引入aspectj lib包
                            <groupId>com.jianliu</groupId>
                            <artifactId>distribute-core</artifactId>
                        </aspectLibrary>
                    </aspectLibraries>
                </configuration>
                <executions>
                    <execution>
                        <!-- IMPORTANT -->
                        <!--<phase>process-sources</phase>-->
                        <goals>
                            <goal>compile</goal>
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

### 代码引入

配置property

    distribute.tx.appName=yesApp
    distribute.tx.serializer=com.jianliu.distributetx.serde.JacksonSerializer

Spring配置

    扫描路径或加载
    com.jianliu.distributetx.config.DistributeTxConfiguration   
    将业务DataSource的id设置为distributeTxDatasource，Router的DataSource或普通DataSource皆可
    如果全局只有一个Datasource，id不强制为distributeTxDatasource
    
代码示例

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


    @DTMethod //声明非事务任务方法，被该注解声明后，会将方法名称、参数写入mysql task记录表，异步执行，要求方法多次执行幂等
    public void unTransactionUpdate(Integer i){
        logger.info("unTransactionUpdate run:" + i);
    }    

### 运行前

执行mvn package打包编译，使aspectj可重新编译带@DTransactional、 @DTMethod 的Class，达到分布式事务增强效果
Aspectj本身无自调用问题，请勿担心    

### 注意点

由于aspectj对版本依赖较为严重，使用时可能需要根据当前jdk版本来调整aspectj版本及插件版本
同时aspectj目前暂时无法和lombok共存
