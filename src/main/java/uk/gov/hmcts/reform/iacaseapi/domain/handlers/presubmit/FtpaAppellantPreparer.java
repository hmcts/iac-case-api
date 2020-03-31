package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo.NO;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo.YES;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.DateProvider;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.*;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;


@Component
public class FtpaAppellantPreparer implements PreSubmitCallbackHandler<AsylumCase> {

    private final DateProvider dateProvider;
    private final int ftpaAppellantAppealOutOfTimeDays;

    public FtpaAppellantPreparer(
        DateProvider dateProvider,
        @Value("${ftpaAppellantAppealOutOfTimeDays}") int ftpaAppellantAppealOutOfTimeDays
    ) {
        this.dateProvider = dateProvider;
        this.ftpaAppellantAppealOutOfTimeDays = ftpaAppellantAppealOutOfTimeDays;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_START
               && callback.getEvent() == Event.APPLY_FOR_FTPA_APPELLANT;
    }

    public PreSubmitCallbackResponse<AsylumCase> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        AsylumCase asylumCase =
            callback
                .getCaseDetails()
                .getCaseData();

        final Optional<String> mayBeAppellantAppealSubmitted = asylumCase.read(FTPA_APPELLANT_SUBMITTED);

        if (mayBeAppellantAppealSubmitted.isPresent() && mayBeAppellantAppealSubmitted.get().equals("Yes")) {
            final PreSubmitCallbackResponse<AsylumCase> asylumCasePreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(asylumCase);
            asylumCasePreSubmitCallbackResponse.addError("You've already submitted an application. You can only make one application at a time.");
            return asylumCasePreSubmitCallbackResponse;
        }

        final Optional<String> mayBeAppealDate = asylumCase.read(APPEAL_DATE);

        if (mayBeAppealDate.filter(s -> dateProvider.now().isAfter(LocalDate.parse(s).plusDays(ftpaAppellantAppealOutOfTimeDays))).isPresent()) {
            asylumCase.write(FTPA_APPELLANT_SUBMISSION_OUT_OF_TIME, YES);
        } else {
            asylumCase.write(FTPA_APPELLANT_SUBMISSION_OUT_OF_TIME, NO);
        }

        return new PreSubmitCallbackResponse<>(asylumCase);
    }

}
