package com.adherelens.clinical.web;

import com.adherelens.clinical.dto.PatientDoseView;
import com.adherelens.clinical.dto.RecordDoseRequest;
import com.adherelens.clinical.service.PatientDoseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No login. The signed token in the path is the only credential, see DoseLinkSigner. */
@RestController
@RequestMapping("/api/public/dose")
public class PublicDoseController {

    private final PatientDoseService patientDoseService;

    public PublicDoseController(PatientDoseService patientDoseService) {
        this.patientDoseService = patientDoseService;
    }

    @GetMapping("/{token}")
    public PatientDoseView view(@PathVariable String token) {
        return patientDoseService.view(token);
    }

    @PostMapping("/{token}/record")
    public PatientDoseView record(@PathVariable String token, @Valid @RequestBody RecordDoseRequest request) {
        return patientDoseService.record(token, request.outcome());
    }
}
