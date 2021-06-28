package com.bcom.nsplacer.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvaluationResults {

    private volatile boolean running;
    private volatile Integer counter;
    private Integer q0Time, q1Time, q2Time, q3Time, q4Time;
    private Integer avgTime;
    private String bwRemaining;
    private String bwUsedPerService;

    public void incrementCounter() {
        counter++;
    }
}
