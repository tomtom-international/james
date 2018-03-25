package com.tomtom.james.newagent.james;

import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JamesObjective;

import java.util.Queue;

public class TextJames extends AbstractJames {
    private static final Logger LOG = Logger.getLogger(TextJames.class);

    public TextJames(Queue<JamesObjective> objectives, int sleepTime) {
        super(objectives, sleepTime);
    }

    public void work(JamesObjective objective) {
        LOG.info("TextJames :: objective ["+objective.getType()+"]" + objective.getClazz().getName() + " :: " + objective.getInformationPoint() + "   |   " + objective.getClazz());
    }

}
