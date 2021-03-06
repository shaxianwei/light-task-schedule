package com.lts.job.tracker.processor;

import com.lts.job.core.protocol.JobProtos;
import com.lts.job.core.protocol.command.JobPullRequest;
import com.lts.job.core.remoting.RemotingServerDelegate;
import com.lts.job.remoting.exception.RemotingCommandException;
import com.lts.job.remoting.protocol.RemotingCommand;
import com.lts.job.tracker.domain.JobTrackerApplication;
import com.lts.job.tracker.support.JobDistributor;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Robert HG (254963746@qq.com) on 7/24/14.
 *         处理 TaskTracker的 Job pull 请求
 */
public class JobPullProcessor extends AbstractProcessor {

    private JobDistributor jobDistributor;

    public JobPullProcessor(RemotingServerDelegate remotingServer, JobTrackerApplication application) {
        super(remotingServer, application);

        jobDistributor = new JobDistributor(application);
    }

    @Override
    public RemotingCommand processRequest(final ChannelHandlerContext ctx, final RemotingCommand request) throws RemotingCommandException {

        JobPullRequest requestBody = request.getBody();

        jobDistributor.distribute(remotingServer, requestBody);

        return RemotingCommand.createResponseCommand(JobProtos.ResponseCode.JOB_PULL_SUCCESS.code(), "");
    }
}
