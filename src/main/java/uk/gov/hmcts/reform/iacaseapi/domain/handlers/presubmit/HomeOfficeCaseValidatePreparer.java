package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.IS_HOME_OFFICE_INTEGRATION_ENABLED;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event.MARK_APPEAL_PAID;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event.PAY_AND_SUBMIT_APPEAL;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event.REQUEST_HOME_OFFICE_DATA;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event.SUBMIT_APPEAL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;

@Component
public class HomeOfficeCaseValidatePreparer implements PreSubmitCallbackHandler<AsylumCase> {

    private final boolean isHomeOfficeIntegrationEnabled;

    public HomeOfficeCaseValidatePreparer(
        @Value("${featureFlag.isHomeOfficeIntegrationEnabled}") boolean isHomeOfficeIntegrationEnabled) {
        this.isHomeOfficeIntegrationEnabled = isHomeOfficeIntegrationEnabled;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_START
            && (callback.getEvent() == SUBMIT_APPEAL
            || callback.getEvent() == PAY_AND_SUBMIT_APPEAL
            || callback.getEvent() == MARK_APPEAL_PAID
            || callback.getEvent() == REQUEST_HOME_OFFICE_DATA);
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        //Check if home office is enabled and set field for CCD definition
        AsylumCase asylumCase = callback.getCaseDetails().getCaseData();

        if (isHomeOfficeIntegrationEnabled) {
            asylumCase.write(IS_HOME_OFFICE_INTEGRATION_ENABLED, YesOrNo.YES);
        } else {
            asylumCase.write(IS_HOME_OFFICE_INTEGRATION_ENABLED, YesOrNo.NO);
        }

        return new PreSubmitCallbackResponse<>(asylumCase);
    }
}
