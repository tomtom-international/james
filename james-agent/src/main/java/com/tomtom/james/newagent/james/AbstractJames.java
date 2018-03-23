package com.tomtom.james.newagent.james;

import com.tomtom.james.newagent.JamesObjective;

import java.util.Queue;

public abstract class AbstractJames implements James {

    private int sleepTime = 1000;
    private Queue<JamesObjective> objectives;

    public AbstractJames(Queue<JamesObjective> objectives, int sleepTime) {
        this.objectives = objectives;
        this.sleepTime = sleepTime;
    }

    abstract public void work(JamesObjective objective);

    @Override
    public void run() {
        while(true) {
            if (objectives.isEmpty()) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                JamesObjective objective = objectives.poll();
                if (objective != null) {
                    work(objective);
                }
            }
        }
    }

}
