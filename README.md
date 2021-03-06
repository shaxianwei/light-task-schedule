LTS 轻量级分布式任务调度框架(Light Task Schedule)
-----------------

## 框架概况：
 LTS是一个轻量级分布式任务调度框架，参考hadoop的部分思想。有三种角色, JobClient, JobTracker, TaskTracker。各个节点都是无状态的，可以部署多个，来实现负载均衡，实现更大的负载量, 并且框架具有很好的容错能力。
 采用多种注册中心（Zookeeper，redis等）进行节点信息暴露，master选举。Mongo存储任务队列和任务执行日志, netty做底层通信。
* JobClient : 主要负责提交任务, 和 接收任务执行反馈结果。
* JobTracker : 负责接收并分配任务，任务调度。
* TaskTracker: 负责执行任务，执行完反馈给JobTracker。

框架支持实时任务，也支持定时任务，同时也支持CronExpression, 有问题，请加QQ群：109500214 一起完善，探讨

##架构图
![Aaron Swartz](https://raw.githubusercontent.com/qq254963746/light-task-schedule/master/data/%E6%9E%B6%E6%9E%84%E5%9B%BE.png)
##节点组:
* 1. 一个节点组等同于一个集群，同一个节点组中的各个节点是对等的，外界无论连接节点组中的任务一个节点都是可以的。
* 2. 每个节点组中都有一个master节点(master宕机，会自动选举出新的master节点)，框架会提供接口API来监听master节点的变化，用户可以自己使用master节点做自己想做的事情。
* 3. JobClient和TaskTracker都可以存在多个节点组。譬如 JobClient 可以存在多个节点组。 譬如：JobClient 节点组为 ‘lts_WEB’ 中的一个节点提交提交一个 只有节点组为’lts_TRADE’的 TaskTracker 才能执行的任务。
* 4. (每个集群中)JobTacker只有一个节点组。
* 5. 多个JobClient节点组和多个TaskTracker节点组再加上一个JobTacker节点组, 组成一个大的集群。

## 工作流程:
* 1. JobClient 提交一个 任务 给 JobTracker, 这里我提供了两种客户端API, 一种是如果JobTracker 不存在或者提交失败，直接返回提交失败。另一种客户端是重试客户端, 如果提交失败，先存储到本地leveldb(可以使用NFS来达到同个节点组共享leveldb文件的目的,多线程访问，做了文件锁处理)，返回给客户端提交成功的信息，待JobTracker可用的时候，再将任务提交。
* 2. JobTracker 收到JobClient提交来的任务，先生成一个唯一的JobID。然后将任务储存在Mongo集群中。JobTracker 发现有（任务执行的）可用的TaskTracker节点(组) 之后，将优先级最大，最先提交的任务分发给TaskTracker。这里JobTracker会优先分配给比较空闲的TaskTracker节点，达到负载均衡。
* 3. TaskTracker 收到JobTracker分发来的任务之后，执行。执行完毕之后，再反馈任务执行结果给JobTracker（成功or 失败[失败有失败错误信息]），如果发现JobTacker不可用，那么存储本地leveldb，等待TaskTracker可用的时候再反馈。反馈结果的同时，询问JobTacker有没有新的任务要执行。
* 4. JobTacker收到TaskTracker节点的任务结果信息，生成并插入(mongo)任务执行日志。根据任务信息决定要不要反馈给客户端。不需要反馈的直接删除, 需要反馈的（同样JobClient不可用存储文件，等待可用重发）。
* 5. JobClient 收到任务执行结果，进行自己想要的逻辑处理。

## 特性

* 负载均衡:
     * JobClient 和 TaskTracker会随机连接JobTracker节点组中的一个节点，实现JobTracker负载均衡。当连接上后，将一直保持连接这个节点,保持连接通道，直到这个节点不可用，减少每次都重新连接一个节点带来的性能开销。
     * JobTracker 分发任务时，是优先分配给最空闲的一个TaskTracker节点，实现TaskTracker节点的负载均衡。

* 健壮性:
     * 当节点组中的一个节点当机之后，自动转到其他节点工作。当整个节点组当机之后，将会采用存储文件的方式，待节点组可用的时候进行重发。
     * 当执行任务的TaskTracker节点当机之后，JobTracker 会将这个TaskTracker上的未完成的任务(死任务)，重新分配给节点组中其他节点执行。

* 伸缩性：
     * 因为各个节点都是无状态的，可以动态增加机器部署实例, 节点关注者会自动发现。
* 扩展性:
     * 采用和dubbo一样的SPI扩展方式，可以实现任务队列扩展，日志记录器扩展等

## 开发计划：
* WEB后台管理
* 框架优化

## 调用示例
* 安装 zookeeper(或redis) 和 mongo(或mysql) (后提供其他任务队列实现方式)

运行 job-example模块中的例子（包含API启动例子和Spring例子）
分别执行 JobTrackerTest TaskTrackerTest JobClientTest

这里给出的是java API(设置配置)方式启动, (spring启动和面添加)

## JobTracker 端
```java
    final JobTracker jobTracker = new JobTracker();
    // 节点信息配置
    jobTracker.setRegistryAddress("zookeeper://127.0.0.1:2181");
    // jobTracker.setRegistryAddress("redis://127.0.0.1:6379");
    jobTracker.setListenPort(35002); // 默认 35001
    jobTracker.setClusterName("test_cluster");
    jobTracker.addMasterChangeListener(new MasterChangeListenerImpl());
    // 设置业务日志记录
    //  jobTracker.addConfig("job.logger", "mongo");
    
    // 1. 任务队列用mongo
    jobTracker.addConfig("job.queue", "mongo");
    // mongo 配置
    jobTracker.addConfig("mongo.addresses", "127.0.0.1:27017");     // 多个地址用逗号分割
    jobTracker.addConfig("mongo.database", "job");
    
    // 2. 任务队里用mysql
    // jobTracker.addConfig("job.queue", "mysql");
    // mysql 配置
    // jobTracker.addConfig("jdbc.url", "jdbc:mysql://test.superboss.cc:3306/lts");
    // jobTracker.addConfig("jdbc.username", "root");
    // jobTracker.addConfig("jdbc.password", "root");
    
    jobTracker.setOldDataHandler(new OldDataDeletePolicy());
    // 设置 zk 客户端用哪个， 可选 zkclient, curator 默认是 zkclient
    jobTracker.addConfig("zk.client", "zkclient");
    // 启动节点
    jobTracker.start();
```

## TaskTracker端
```java
    TaskTracker taskTracker = new TaskTracker();
    taskTracker.setJobRunnerClass(TestJobRunner.class);
    // jobClient.setClusterName("lts");
    taskTracker.setRegistryAddress("zookeeper://127.0.0.1:2181");
    // taskTracker.setRegistryAddress("redis://127.0.0.1:6379");
    taskTracker.setNodeGroup("test_trade_TaskTracker");
    taskTracker.setClusterName("test_cluster");
    taskTracker.setWorkThreads(20);
    taskTracker.start();
    // 任务执行类
    public class TestJobRunner implements JobRunner {
        @Override
        public void run(Job job) throws Throwable {
            System.out.println("我要执行"+ job);
            System.out.println(job.getParam("shopId"));
            try {
                Thread.sleep(5*1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
```

## JobClient端
```java
    JobClient jobClient = new RetryJobClient();
    // final JobClient jobClient = new JobClient();
    jobClient.setNodeGroup("test_jobClient");
    jobClient.setClusterName("test_cluster");
    jobClient.setRegistryAddress("zookeeper://127.0.0.1:2181");
    // jobClient.setRegistryAddress("redis://127.0.0.1:6379");
    // 提交任务
    Job job = new Job();
    job.setTaskId("3213213123");
    job.setParam("shopId", "11111");
    job.setTaskTrackerNodeGroup("test_trade_TaskTracker");
    // job.setCronExpression("0 0/1 * * * ?");  // 支持 cronExpression表达式
    // job.setTriggerTime(new Date().getTime()); // 支持指定时间执行
    Response response = jobClient.submitJob(job);
```


