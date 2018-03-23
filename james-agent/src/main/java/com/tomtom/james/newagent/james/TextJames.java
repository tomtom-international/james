package com.tomtom.james.newagent.james;

import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JamesObjective;

import java.util.Queue;

public class TextJames implements James {
    private static final Logger LOG = Logger.getLogger(TextJames.class);
    private int sleepTime = 1000;
    private Queue<JamesObjective> objectives;

    public TextJames(Queue<JamesObjective> objectives, int sleepTime) {
        this.objectives = objectives;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        LOG.trace("TextJames starts running.");
        while(true) {
            if (objectives.isEmpty()) {
                LOG.trace("TextJames - no objectives in queue - sleeps " + sleepTime + " ms");
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            JamesObjective objective = objectives.poll();
            if (objective != null) {
                LOG.info("TextJames :: objective " + objective.getClazz().getName() + " :: " + objective.getInformationPoint() + "   |   " + objective.getClazz());
            }
        }
    }
}
