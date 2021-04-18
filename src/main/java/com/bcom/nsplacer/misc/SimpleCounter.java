package com.bcom.nsplacer.misc;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class SimpleCounter {

    private int cnt = 0;

    public void increment() {
        cnt++;
    }
}
