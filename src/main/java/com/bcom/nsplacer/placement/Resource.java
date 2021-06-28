package com.bcom.nsplacer.placement;

import com.bcom.nsplacer.placement.enums.ResourceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Resource {

    private ResourceType type;
    private int value;

    public Resource(ResourceType type, Integer value) {
        this.type = type;
        this.value = value;
    }

    public Resource clone() {
        return new Resource(type, value);
    }
}
