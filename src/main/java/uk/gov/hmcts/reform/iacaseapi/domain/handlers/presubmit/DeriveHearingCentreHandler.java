package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo.NO;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo.YES;

import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.HearingCentre;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.casemanagementlocation.BaseLocation;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.casemanagementlocation.CaseManagementLocation;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.casemanagementlocation.Region;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.AddressUk;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.iacaseapi.domain.service.HearingCentreFinder;
import uk.gov.hmcts.reform.iacaseapi.infrastructure.utils.HearingCenterMapper;

@Component
public class DeriveHearingCentreHandler implements PreSubmitCallbackHandler<AsylumCase> {

    private final HearingCentreFinder hearingCentreFinder;

    public DeriveHearingCentreHandler(
        HearingCentreFinder hearingCentreFinder
    ) {
        this.hearingCentreFinder = hearingCentreFinder;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT
            && Arrays.asList(
            Event.SUBMIT_APPEAL,
            Event.EDIT_APPEAL_AFTER_SUBMIT,
            Event.PAY_AND_SUBMIT_APPEAL)
            .contains(callback.getEvent());
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final AsylumCase asylumCase =
            callback
                .getCaseDetails()
                .getCaseData();

//        if (asylumCase.read(HEARING_CENTRE).isEmpty()
//            || Event.EDIT_APPEAL_AFTER_SUBMIT.equals(callback.getEvent())) {

            trySetHearingCentreFromPostcode(asylumCase);
//        }

        return new PreSubmitCallbackResponse<>(asylumCase);
    }

    private Optional<String> getAppellantPostcode(
        AsylumCase asylumCase
    ) {
        if (asylumCase.read(APPELLANT_HAS_FIXED_ADDRESS, YesOrNo.class)
            .orElse(NO) == YES) {

            Optional<AddressUk> optionalAppellantAddress = asylumCase.read(APPELLANT_ADDRESS);

            if (optionalAppellantAddress.isPresent()) {

                AddressUk appellantAddress = optionalAppellantAddress.get();

                return appellantAddress.getPostCode();
            }
        }

        return Optional.empty();
    }

    private void trySetHearingCentreFromPostcode(
        AsylumCase asylumCase
    ) {
        Optional<String> optionalAppellantPostcode = getAppellantPostcode(asylumCase);

        if (optionalAppellantPostcode.isPresent()) {
            String appellantPostcode = optionalAppellantPostcode.get();
            HearingCentre hearingCentre = hearingCentreFinder.find(appellantPostcode);
            asylumCase.write(HEARING_CENTRE, hearingCentre);
            asylumCase.write(APPLICATION_CHANGE_DESIGNATED_HEARING_CENTRE, hearingCentre);
            asylumCase.write(AsylumCaseFieldDefinition.CASE_MANAGEMENT_LOCATION,
                getCaseManagementLocation(hearingCentre));
        } else {
            asylumCase.write(HEARING_CENTRE, hearingCentreFinder.getDefaultHearingCentre());
            asylumCase.write(APPLICATION_CHANGE_DESIGNATED_HEARING_CENTRE,
                hearingCentreFinder.getDefaultHearingCentre());
            asylumCase.write(AsylumCaseFieldDefinition.CASE_MANAGEMENT_LOCATION,
                getCaseManagementLocation(hearingCentreFinder.getDefaultHearingCentre()));
        }
    }

    private CaseManagementLocation getCaseManagementLocation(HearingCentre hearingCentre) {
        return new CaseManagementLocation(Region.NATIONAL, HearingCenterMapper.getBaseLocation(hearingCentre));
    }
}
