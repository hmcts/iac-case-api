package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.CASE_MANAGEMENT_LOCATION;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.HEARING_CENTRE;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.STAFF_LOCATION;

import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.CaseManagementLocation;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.HearingCentre;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.infrastructure.utils.StaffLocation;

@Component
public class AddBaseLocationFromHearingCentreForOldCasesFixHandler implements PreSubmitCallbackHandler<AsylumCase> {

    private final CaseManagementLocationService caseManagementLocationService;

    public AddBaseLocationFromHearingCentreForOldCasesFixHandler(
        CaseManagementLocationService caseManagementLocationService) {
        this.caseManagementLocationService = caseManagementLocationService;
    }

    @Override
    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_START
            && !Arrays.asList(
            Event.SUBMIT_APPEAL,
            Event.EDIT_APPEAL_AFTER_SUBMIT,
            Event.PAY_AND_SUBMIT_APPEAL,
            Event.CHANGE_HEARING_CENTRE
        ).contains(callback.getEvent());
    }

    public DispatchPriority getDispatchPriority() {
        return DispatchPriority.EARLIEST;
    }

    @Override
    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        AsylumCase asylumCase = callback.getCaseDetails().getCaseData();

        HearingCentre hearingCentre = asylumCase.read(HEARING_CENTRE, HearingCentre.class)
            .orElse(HearingCentre.TAYLOR_HOUSE);

        Optional<CaseManagementLocation> caseManagementLocation =
            asylumCase.read(CASE_MANAGEMENT_LOCATION, CaseManagementLocation.class);

        if (caseManagementLocation.isEmpty()
            || StringUtils.isBlank(caseManagementLocation.get().getBaseLocation().name())) {
            addBaseLocationAndStaffLocationFromHearingCentre(asylumCase, hearingCentre);
        }

        return new PreSubmitCallbackResponse<>(asylumCase);
    }

    private void addBaseLocationAndStaffLocationFromHearingCentre(AsylumCase asylumCase, HearingCentre hearingCentre) {
        String staffLocationName = StaffLocation.getLocation(hearingCentre).getName();
        asylumCase.write(STAFF_LOCATION, staffLocationName);
        asylumCase.write(CASE_MANAGEMENT_LOCATION,
            caseManagementLocationService.getCaseManagementLocation(staffLocationName));
    }

}

