package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.ADJOURN_HEARING_WITHOUT_DATE_REASONS;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.DATE_BEFORE_ADJOURN_WITHOUT_DATE;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.DOES_THE_CASE_NEED_TO_BE_RELISTED;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.LIST_CASE_HEARING_DATE;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.STATE_BEFORE_ADJOURN_WITHOUT_DATE;

import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.State;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;


@ExtendWith(MockitoExtension.class)
public class RestoreStateFromAdjournHandlerTest {

    private final String listCaseHearingDate = "05/05/2020";
    @Mock
    private Callback<AsylumCase> callback;
    @Mock
    private CaseDetails<AsylumCase> caseDetails;
    @Mock
    private AsylumCase asylumCase;
    @Mock
    private PreSubmitCallbackResponse<AsylumCase> callbackResponse;
    private RestoreStateFromAdjournHandler restoreStateFromAdjournHandler;

    @BeforeEach
    public void setUp() {
        restoreStateFromAdjournHandler = new RestoreStateFromAdjournHandler();
    }

    @Test
    public void should_return_updated_state_for_return_state_from_adjourn_adjourned_state() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(Event.RESTORE_STATE_FROM_ADJOURN);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(STATE_BEFORE_ADJOURN_WITHOUT_DATE, String.class))
            .thenReturn(Optional.of(State.PREPARE_FOR_HEARING.toString()));
        when(asylumCase.read(DATE_BEFORE_ADJOURN_WITHOUT_DATE, String.class))
            .thenReturn(Optional.of(listCaseHearingDate));

        PreSubmitCallbackResponse<AsylumCase> returnedCallbackResponse =
            restoreStateFromAdjournHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, callback, callbackResponse);

        assertNotNull(returnedCallbackResponse);
        Assertions.assertThat(returnedCallbackResponse.getState()).isEqualTo(State.PREPARE_FOR_HEARING);
        assertEquals(asylumCase, returnedCallbackResponse.getData());

        verify(asylumCase, times(1)).write(DOES_THE_CASE_NEED_TO_BE_RELISTED, YesOrNo.YES);
        verify(asylumCase, times(1)).write(LIST_CASE_HEARING_DATE, listCaseHearingDate);
        verify(asylumCase, times(1)).clear(DATE_BEFORE_ADJOURN_WITHOUT_DATE);
        verify(asylumCase, times(1)).clear(STATE_BEFORE_ADJOURN_WITHOUT_DATE);
        verify(asylumCase, times(1)).clear(ADJOURN_HEARING_WITHOUT_DATE_REASONS);
    }

    @Test
    public void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> restoreStateFromAdjournHandler
            .handle(PreSubmitCallbackStage.ABOUT_TO_START, callback, callbackResponse))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void it_can_handle_callback() {

        for (Event event : Event.values()) {

            when(callback.getEvent()).thenReturn(event);

            for (PreSubmitCallbackStage callbackStage : PreSubmitCallbackStage.values()) {

                boolean canHandle = restoreStateFromAdjournHandler.canHandle(callbackStage, callback);

                if (event == Event.RESTORE_STATE_FROM_ADJOURN
                    && callbackStage == PreSubmitCallbackStage.ABOUT_TO_SUBMIT) {

                    assertTrue(canHandle);
                } else {
                    assertFalse(canHandle);
                }
            }

            reset(callback);
        }
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> restoreStateFromAdjournHandler.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> restoreStateFromAdjournHandler.canHandle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> restoreStateFromAdjournHandler.handle(null, callback, callbackResponse))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(
            () -> restoreStateFromAdjournHandler.handle(PreSubmitCallbackStage.ABOUT_TO_SUBMIT, null, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
