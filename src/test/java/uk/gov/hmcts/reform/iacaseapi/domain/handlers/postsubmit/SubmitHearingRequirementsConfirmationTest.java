package uk.gov.hmcts.reform.iacaseapi.domain.handlers.postsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PostSubmitCallbackResponse;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SubmitHearingRequirementsConfirmationTest {

    @Mock private Callback<AsylumCase> callback;

    SubmitHearingRequirementsConfirmation submitHearingRequirementsConfirmation =
        new SubmitHearingRequirementsConfirmation();

    @Test
    void should_return_confirmation() {

        when(callback.getEvent()).thenReturn(Event.DRAFT_HEARING_REQUIREMENTS);

        PostSubmitCallbackResponse callbackResponse =
            submitHearingRequirementsConfirmation.handle(callback);

        assertNotNull(callbackResponse);
        assertTrue(callbackResponse.getConfirmationHeader().isPresent());
        assertTrue(callbackResponse.getConfirmationBody().isPresent());

        assertThat(
            callbackResponse.getConfirmationHeader().get())
            .contains("You've submitted your hearing requirements");

        assertThat(
            callbackResponse.getConfirmationBody().get())
            .contains("The Tribunal will review your hearing requirements and any additional requests for adjustments.<br><br>");

        assertThat(
            callbackResponse.getConfirmationBody().get())
            .contains("We'll notify you when the hearing is listed. You'll then be able to review the hearing requirements.");
    }

    @Test
    void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> submitHearingRequirementsConfirmation.handle(callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            boolean canHandle = submitHearingRequirementsConfirmation.canHandle(callback);

            if (event == Event.DRAFT_HEARING_REQUIREMENTS) {

                assertTrue(canHandle);
            } else {
                assertFalse(canHandle);
            }

            reset(callback);
        }
    }

    @Test
    void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> submitHearingRequirementsConfirmation.canHandle(null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> submitHearingRequirementsConfirmation.handle(null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
