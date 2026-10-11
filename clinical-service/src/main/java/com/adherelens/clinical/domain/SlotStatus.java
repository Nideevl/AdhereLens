package com.adherelens.clinical.domain;

public enum SlotStatus {
    PENDING,
    HIT,
    DELAYED,
    MISSED,
    REFUSED,
    CANCELLED   // regimen was discontinued before this dose came due; never published as an outcome
}
