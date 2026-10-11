package com.adherelens.clinical.dto;

import com.adherelens.common.model.SideEffect;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** One or more side effects from the fixed list, all observed for the same regimen. */
public record SideEffectReportRequest(
        @NotEmpty @Size(max = 10) List<@NotNull SideEffect> sideEffects
) {
}
