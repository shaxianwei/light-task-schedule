package com.lts.job.tracker.support.policy;

import com.lts.job.queue.JobFeedbackQueue;
import com.lts.job.queue.domain.JobFeedbackPo;
import com.lts.job.tracker.support.OldDataHandler;

/**
 * @author Robert HG (254963746@qq.com) on 3/30/15.
 */
public class OldDataDeletePolicy implements OldDataHandler {

    private long expired = 30 * 24 * 60 * 60 * 1000L;        // 默认30 天

    public OldDataDeletePolicy() {
    }

    public OldDataDeletePolicy(long expired) {
        this.expired = expired;
    }

    public boolean handleJobFeedbackPo(JobFeedbackQueue jobFeedbackQueue, JobFeedbackPo jobFeedbackPo, JobFeedbackPo po) {

        if (System.currentTimeMillis() - jobFeedbackPo.getGmtCreated() > expired) {
            // delete
            jobFeedbackQueue.remove(po.getId());
            return true;
        }

        return false;
    }
}
