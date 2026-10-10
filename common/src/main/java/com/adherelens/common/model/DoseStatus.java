package com.adherelens.common.model;

public enum DoseStatus {
    HIT,        // taken/administered at or before the due time
    DELAYED,    // taken/administered after the due time but inside the grace window
    MISSED,     // no response when the grace window closed (system-generated)
    REFUSED     // patient refused; recorded by a nurse only
}
