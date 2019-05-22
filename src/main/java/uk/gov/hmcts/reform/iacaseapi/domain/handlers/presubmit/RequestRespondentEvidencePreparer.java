package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumExtractor.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.DateProvider;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.CaseDataMap;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.Parties;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PreSubmitCallbackHandler;

@Component
public class RequestRespondentEvidencePreparer implements PreSubmitCallbackHandler<CaseDataMap> {

    private final int requestRespondentEvidenceDueInDays;
    private final DateProvider dateProvider;

    public RequestRespondentEvidencePreparer(
        @Value("${requestRespondentEvidence.dueInDays}") int requestRespondentEvidenceDueInDays,
        DateProvider dateProvider
    ) {
        this.requestRespondentEvidenceDueInDays = requestRespondentEvidenceDueInDays;
        this.dateProvider = dateProvider;
    }

    public boolean canHandle(
        PreSubmitCallbackStage callbackStage,
        Callback<CaseDataMap> callback
    ) {
        requireNonNull(callbackStage, "callbackStage must not be null");
        requireNonNull(callback, "callback must not be null");

        return callbackStage == PreSubmitCallbackStage.ABOUT_TO_START
               && callback.getEvent() == Event.REQUEST_RESPONDENT_EVIDENCE;
    }

    public PreSubmitCallbackResponse<CaseDataMap> handle(
        PreSubmitCallbackStage callbackStage,
        Callback<CaseDataMap> callback
    ) {
        if (!canHandle(callbackStage, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDataMap caseDataMap =
            callback
                .getCaseDetails()
                .getCaseData();

        caseDataMap.write(SEND_DIRECTION_EXPLANATION,
            "A notice of appeal has been lodged against this asylum decision.\n\n"
            + "You must now send all documents to the case officer. The case officer will send them to the other party. "
            + "You have " + requestRespondentEvidenceDueInDays + " days to supply these documents.\n\n"
            + "You must include:\n"
            + "- the notice of decision\n"
            + "- any other document provided to the appellant giving reasons for that decision\n"
            + "- any statements of evidence\n"
            + "- the application form\n"
            + "- any record of interview with the appellant in relation to the decision being appealed\n"
            + "- any other unpublished documents on which you rely\n"
            + "- the notice of any other appealable decision made in relation to the appellant"
        );

        caseDataMap.write(SEND_DIRECTION_PARTIES, Parties.RESPONDENT);

        caseDataMap.write(SEND_DIRECTION_DATE_DUE,
            dateProvider
                .now()
                .plusDays(requestRespondentEvidenceDueInDays)
                .toString()
        );

        return new PreSubmitCallbackResponse<>(caseDataMap);
    }
}
