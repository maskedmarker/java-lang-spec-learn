package org.example.learn.java.spec.commons.model;

import org.example.learn.java.spec.commons.model.anno.Executor;
import org.example.learn.java.spec.commons.model.anno.Schedule;

public class TaskPlan {

    @Schedule(jobName = "job1", cron = "11111")
    @Schedule(jobName = "job2", cron = "22222")
    @Executor("thread-pool-task")
    public void perform() {
    }
}
