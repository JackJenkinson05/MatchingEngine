package com.jackjenkinson.MatchingEngineSolutions;

import com.jackjenkinson.MatchingEngine;
import java.util.function.Supplier;

public enum EngineSolution {

    SOLUTION_1("Solution 1 — TreeMap + LinkedList (baseline)", MatchingEngineSolution1::new);

    private final String description;
    private final Supplier<MatchingEngine> factory;

    EngineSolution(String description, Supplier<MatchingEngine> factory) {
        this.description = description;
        this.factory = factory;
    }

    /** Creates a fresh, empty engine for this solution. */
    public MatchingEngine create() {
        return factory.get();
    }

    public String description() {
        return description;
    }
}
