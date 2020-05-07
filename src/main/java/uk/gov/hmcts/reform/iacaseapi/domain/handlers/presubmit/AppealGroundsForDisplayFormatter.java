package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CheckValues;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;

@Component
public class AppealGroundsForDisplayFormatter implements PreSubmitCallbackHandler<AsylumCase> {

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT
               && (callback.getEvent() == Event.START_APPEAL
                   || callback.getEvent() == Event.EDIT_APPEAL);
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

        Set<String> appealGrounds = new LinkedHashSet<>();

        Optional<CheckValues<String>> maybeAppealGroundsProtection = asylumCase.read(APPEAL_GROUNDS_PROTECTION);

        maybeAppealGroundsProtection
            .ifPresent(appealGroundsProtection ->
                appealGrounds.addAll(appealGroundsProtection.getValues()));

        Optional<CheckValues<String>> maybeAppealGroundsHumanRights = asylumCase.read(APPEAL_GROUNDS_HUMAN_RIGHTS);

        maybeAppealGroundsHumanRights.ifPresent(appealGroundsHumanRights ->
                appealGrounds.addAll(appealGroundsHumanRights.getValues()));

        Optional<CheckValues<String>> maybeAppealGroundsRevocation = asylumCase.read(APPEAL_GROUNDS_REVOCATION);

        maybeAppealGroundsRevocation
            .ifPresent(appealGroundsRevocation -> appealGrounds.addAll(appealGroundsRevocation.getValues()));

        Optional<CheckValues<String>> maybeAppealGroundsHumanRightsRefusal = asylumCase.read(APPEAL_GROUNDS_HUMAN_RIGHTS_REFUSAL);

        maybeAppealGroundsHumanRightsRefusal.ifPresent(appealGroundsHumanRightsRefusal ->
            appealGrounds.addAll(appealGroundsHumanRightsRefusal.getValues()));

        Optional<CheckValues<String>> maybeAppealGroundsDeprivationHumanRights = asylumCase.read(APPEAL_GROUNDS_DEPRIVATION_HUMAN_RIGHTS);

        maybeAppealGroundsDeprivationHumanRights.ifPresent(appealGroundsDeprivationHumanRights ->
            appealGrounds.addAll(appealGroundsDeprivationHumanRights.getValues()));

        Optional<CheckValues<String>> maybeAppealGroundsDeprivation = asylumCase.read(APPEAL_GROUNDS_DEPRIVATION);

        maybeAppealGroundsDeprivation.ifPresent(appealGroundsDeprivation ->
            appealGrounds.addAll(appealGroundsDeprivation.getValues()));

        Optional<CheckValues<String>> maybeAppealGroundsEuRefusal = asylumCase.read(APPEAL_GROUNDS_EU_REFUSAL);

        maybeAppealGroundsEuRefusal.ifPresent(appealGroundsEuRefusal ->
            appealGrounds.addAll(appealGroundsEuRefusal.getValues()));

        asylumCase.write(
            APPEAL_GROUNDS_FOR_DISPLAY,
            new ArrayList<>(appealGrounds)
        );

        asylumCase.clear(APPEAL_GROUNDS_PROTECTION);
        asylumCase.clear(APPEAL_GROUNDS_HUMAN_RIGHTS);
        asylumCase.clear(APPEAL_GROUNDS_REVOCATION);
        asylumCase.clear(APPEAL_GROUNDS_HUMAN_RIGHTS_REFUSAL);
        asylumCase.clear(APPEAL_GROUNDS_DEPRIVATION_HUMAN_RIGHTS);
        asylumCase.clear(APPEAL_GROUNDS_DEPRIVATION);
        asylumCase.clear(APPEAL_GROUNDS_EU_REFUSAL);

        return new PreSubmitCallbackResponse<>(asylumCase);
    }
}
