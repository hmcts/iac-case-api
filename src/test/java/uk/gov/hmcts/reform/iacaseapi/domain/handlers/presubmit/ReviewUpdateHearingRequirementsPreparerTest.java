package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.*;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage.ABOUT_TO_START;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackStage.ABOUT_TO_SUBMIT;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.InterpreterLanguage;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.WitnessDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.Callback;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.YesOrNo;

@ExtendWith(MockitoExtension.class)
class ReviewUpdateHearingRequirementsPreparerTest {

    @Mock private Callback<AsylumCase> callback;
    @Mock private CaseDetails<AsylumCase> caseDetails;
    @Mock private AsylumCase asylumCase;
    @Mock private List<IdValue<WitnessDetails>> witnessDetails;
    @Mock private List<IdValue<InterpreterLanguage>> interpreterLanguage;

    ReviewUpdateHearingRequirementsPreparer reviewUpdateHearingRequirementsPreparer;

    @BeforeEach
    void setUp() {

        reviewUpdateHearingRequirementsPreparer =
            new ReviewUpdateHearingRequirementsPreparer();
    }

    @Test
    void should_review_updated_hearing_requirements() {

        when(callback.getEvent()).thenReturn(Event.UPDATE_HEARING_ADJUSTMENTS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(UPDATE_HEARING_REQUIREMENTS_EXISTS, YesOrNo.class)).thenReturn(Optional.of(YesOrNo.YES));

        witnessDetails = Arrays.asList(
            new IdValue<>("1", new WitnessDetails("Witness1")),
            new IdValue<>("2", new WitnessDetails("Witness2"))
        );

        interpreterLanguage = Arrays.asList(
            new IdValue<>("1", new InterpreterLanguage("Irish", "N/A"))
        );

        when(asylumCase.read(WITNESS_DETAILS)).thenReturn(Optional.of(witnessDetails));
        when(asylumCase.read(INTERPRETER_LANGUAGE)).thenReturn(Optional.of(interpreterLanguage));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            reviewUpdateHearingRequirementsPreparer.handle(ABOUT_TO_START, callback);

        assertNotNull(callback);
        assertEquals(asylumCase, callbackResponse.getData());

        verify(asylumCase, times(1)).read(WITNESS_DETAILS);
        verify(asylumCase, times(1)).read(INTERPRETER_LANGUAGE);

        verify(asylumCase, times(1)).write(
            WITNESS_DETAILS_READONLY,
            "Name\t\tWitness1\nName\t\tWitness2");
        verify(asylumCase, times(1)).write(
            INTERPRETER_LANGUAGE_READONLY,
            "Language\t\tIrish\nDialect\t\t\tN/A\n");
    }

    @Test
    void should_error_when_updated_hearing_requirements_is_not_available() {

        when(callback.getEvent()).thenReturn(Event.UPDATE_HEARING_ADJUSTMENTS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(UPDATE_HEARING_REQUIREMENTS_EXISTS, YesOrNo.class)).thenReturn(Optional.of(YesOrNo.NO));

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            reviewUpdateHearingRequirementsPreparer.handle(ABOUT_TO_START, callback);

        assertNotNull(callback);
        assertEquals(asylumCase, callbackResponse.getData());
        final Set<String> errors = callbackResponse.getErrors();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(errors).containsOnly("You've made an invalid request. You must update the Hearing requirements before you can update the adjustments.");

        verify(asylumCase, never()).read(WITNESS_DETAILS);
        verify(asylumCase, never()).read(INTERPRETER_LANGUAGE);

        verify(asylumCase, never()).write(
            eq(WITNESS_DETAILS_READONLY),
            anyString());
        verify(asylumCase, never()).write(
            eq(INTERPRETER_LANGUAGE_READONLY),
            anyString());
    }


    @Test
    void should_error_when_updated_hearing_requirements_flag_is_not_available() {

        when(callback.getEvent()).thenReturn(Event.UPDATE_HEARING_ADJUSTMENTS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(asylumCase);
        when(asylumCase.read(UPDATE_HEARING_REQUIREMENTS_EXISTS, YesOrNo.class)).thenReturn(Optional.empty());

        PreSubmitCallbackResponse<AsylumCase> callbackResponse =
            reviewUpdateHearingRequirementsPreparer.handle(ABOUT_TO_START, callback);

        assertNotNull(callback);
        assertEquals(asylumCase, callbackResponse.getData());
        final Set<String> errors = callbackResponse.getErrors();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(errors).containsOnly("You've made an invalid request. You must update the Hearing requirements before you can update the adjustments.");

        verify(asylumCase, never()).read(WITNESS_DETAILS);
        verify(asylumCase, never()).read(INTERPRETER_LANGUAGE);

        verify(asylumCase, never()).write(
            eq(WITNESS_DETAILS_READONLY),
            anyString());
        verify(asylumCase, never()).write(
            eq(INTERPRETER_LANGUAGE_READONLY),
            anyString());
    }


    @Test
    void handling_should_throw_if_cannot_actually_handle() {

        assertThatThrownBy(() -> reviewUpdateHearingRequirementsPreparer.handle(ABOUT_TO_SUBMIT, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);

        when(callback.getEvent()).thenReturn(Event.LIST_CASE);
        assertThatThrownBy(() -> reviewUpdateHearingRequirementsPreparer.handle(ABOUT_TO_START, callback))
            .hasMessage("Cannot handle callback")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> reviewUpdateHearingRequirementsPreparer.canHandle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> reviewUpdateHearingRequirementsPreparer.canHandle(ABOUT_TO_START, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> reviewUpdateHearingRequirementsPreparer.handle(null, callback))
            .hasMessage("callbackStage must not be null")
            .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> reviewUpdateHearingRequirementsPreparer.handle(ABOUT_TO_START, null))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
