package com.adherelens.analytics.web;

import com.adherelens.analytics.dto.AnalyticsDtos.DayPoint;
import com.adherelens.analytics.dto.AnalyticsDtos.DrugRow;
import com.adherelens.analytics.dto.AnalyticsDtos.HospitalRow;
import com.adherelens.analytics.dto.AnalyticsDtos.Result;
import com.adherelens.analytics.dto.AnalyticsDtos.SideEffectRow;
import com.adherelens.analytics.dto.AnalyticsDtos.SummaryRow;
import com.adherelens.analytics.service.AnalyticsQueryService;
import com.adherelens.auth.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * scope "hospital" = the caller's own hospital. scope "network" = all hospitals together.
 * Who may use which scope is decided in SecurityConfig.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{1,100}");

    private final AnalyticsQueryService queries;

    public AnalyticsController(AnalyticsQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/{scope}/drugs")
    public Result<String> drugs(@AuthenticationPrincipal AuthUser caller, @PathVariable String scope) {
        return queries.drugs(hospitalFor(scope, caller));
    }

    @GetMapping("/{scope}/summary")
    public Result<SummaryRow> summary(@AuthenticationPrincipal AuthUser caller, @PathVariable String scope,
                                      @RequestParam(required = false) String drugName) {
        return queries.summary(hospitalFor(scope, caller), code(drugName));
    }

    @GetMapping("/{scope}/adherence-by-day")
    public Result<DayPoint> adherenceByDay(@AuthenticationPrincipal AuthUser caller, @PathVariable String scope,
                                           @RequestParam(defaultValue = "OUTPATIENT") String careSetting,
                                           @RequestParam(required = false) String drugName,
                                           @RequestParam(defaultValue = "30") int maxDay) {
        return queries.adherenceByDay(hospitalFor(scope, caller), setting(careSetting), code(drugName),
                Math.min(Math.max(maxDay, 1), 90));
    }

    @GetMapping("/{scope}/adherence-by-drug")
    public Result<DrugRow> adherenceByDrug(@AuthenticationPrincipal AuthUser caller, @PathVariable String scope,
                                           @RequestParam(defaultValue = "OUTPATIENT") String careSetting) {
        return queries.adherenceByDrug(hospitalFor(scope, caller), setting(careSetting));
    }

    @GetMapping("/{scope}/side-effects")
    public Result<SideEffectRow> sideEffects(@AuthenticationPrincipal AuthUser caller, @PathVariable String scope,
                                             @RequestParam(required = false) String drugName) {
        return queries.sideEffects(hospitalFor(scope, caller), code(drugName));
    }

    @GetMapping("/network/hospital-comparison")
    public Result<HospitalRow> hospitalComparison(@AuthenticationPrincipal AuthUser caller,
                                                  @RequestParam String drugName,
                                                  @RequestParam(defaultValue = "OUTPATIENT") String careSetting) {
        return queries.hospitalComparison(caller.hospitalId(), setting(careSetting), code(drugName));
    }

    private static UUID hospitalFor(String scope, AuthUser caller) {
        return switch (scope) {
            case "hospital" -> caller.hospitalId();
            case "network" -> null;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown scope");
        };
    }

    private static String setting(String value) {
        if (!value.equals("INPATIENT") && !value.equals("OUTPATIENT")) {
            throw new IllegalArgumentException("careSetting must be INPATIENT or OUTPATIENT");
        }
        return value;
    }

    private static String code(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("drugName must be upper-case letters, digits or underscore");
        }
        return value;
    }
}
