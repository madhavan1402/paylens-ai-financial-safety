package com.paylens.backend.model;

/** A neutral financial fact produced by a simulation, not a policy decision. */
public enum SimulationConsequence {
    NORMAL,
    RESERVE_BREACH,
    OBLIGATION_SHORTFALL
}
