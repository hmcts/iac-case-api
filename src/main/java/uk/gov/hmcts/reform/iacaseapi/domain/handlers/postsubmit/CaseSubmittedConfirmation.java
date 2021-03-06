package uk.gov.hmcts.reform.iacaseapi.domain.handlers.postsubmit;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PostSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.PostSubmitCallbackHandler;

@Component
public class CaseSubmittedConfirmation implements PostSubmitCallbackHandler<AsylumCase> {

    @Value("${featureFlag.isSaveAndContinueEnabled}")
    private boolean isSaveAndContinueEnabled;

    public boolean canHandle(
        Callback<AsylumCase> callback
    ) {
        requireNonNull(callback, "callback must not be null");

        Event validEvent = isSaveAndContinueEnabled ? Event.SUBMIT_CASE : Event.BUILD_CASE;
        return callback.getEvent() == validEvent;
    }

    public PostSubmitCallbackResponse handle(
        Callback<AsylumCase> callback
    ) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        PostSubmitCallbackResponse postSubmitResponse =
            new PostSubmitCallbackResponse();

        postSubmitResponse.setConfirmationHeader("# You have submitted your case");
        postSubmitResponse.setConfirmationBody(
            "We have sent you a confirmation email\n\n"
            + "#### What happens next\n"
            + "The case officer will now review your appeal. "
            + "If it complies with the procedure rules and practice directions, they will send it to the respondent for them to review."
        );

        return postSubmitResponse;
    }
}
