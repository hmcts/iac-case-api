package uk.gov.hmcts.reform.iacaseapi.domain.handlers.presubmit.editdocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.FINAL_DECISION_AND_REASONS_DOCUMENTS;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition.FINAL_DECISION_AND_REASONS_PDF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.lang.Nullable;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCase;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.AsylumCaseFieldDefinition;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.DocumentTag;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.DocumentWithMetadata;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.Document;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.field.IdValue;
import uk.gov.hmcts.reform.iacaseapi.domain.handlers.postsubmit.editdocs.EditDocsAuditService;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@ExtendWith(MockitoExtension.class)
class EditDocsServiceTest {

    public static final String DOC_ID = "5c39421f-e518-49da-987b-c4c48dffab43";
    public static final String ANOTHER_DOC_ID = "ad22bb0f-5b5a-4a39-bd6d-b966e8602072";
    @Mock
    private EditDocsAuditService editDocsAuditService;
    @InjectMocks
    private EditDocsService editDocsService;

    @ParameterizedTest
    @MethodSource({
        "documentIsDeletedScenario",
        "anotherDocumentIsDeletedScenario",
        "theOverviewTabFieldIsNullScenario",
        "documentIsUpdatedScenario"
    })
    void cleanUpOverviewTabDocs(AsylumCase asylum,
                                       AsylumCase asylumBefore,
                                       List<String> updatedDeletedDocIdsList,
                                       @Nullable Document expectedDocument) {

        given(editDocsAuditService.getUpdatedAndDeletedDocIdsForGivenField(any(), any(),
            eq(FINAL_DECISION_AND_REASONS_DOCUMENTS))).willReturn(updatedDeletedDocIdsList);

        editDocsService.cleanUpOverviewTabDocs(asylum, asylumBefore);

        Document actualFinalDecisionAndReasonPdf = asylum.read(FINAL_DECISION_AND_REASONS_PDF, Document.class)
            .orElse(null);
        assertThat(actualFinalDecisionAndReasonPdf).isEqualTo(expectedDocument);
    }

    private static Object[] documentIsDeletedScenario() {
        AsylumCase asylumCase = buildCaseWithPopulatedFieldForGivenDoc(buildDocumentGivenDocId());

        return new Object[] {
            new Object[] {
                asylumCase,
                new AsylumCase(),
                new ArrayList<>(Collections.singletonList(DOC_ID)),
                null}
        };
    }

    private static Object[] anotherDocumentIsDeletedScenario() {
        AsylumCase asylumCase = buildCaseWithPopulatedFieldForGivenDoc(buildDocumentGivenDocId());
        asylumCase.write(FINAL_DECISION_AND_REASONS_DOCUMENTS, Collections.singletonList(buildIdValue()));
        Document expectedFinalDecisionAndReasonPdf = buildDocumentGivenDocId();

        return new Object[] {
            new Object[] {
                asylumCase,
                new AsylumCase(),
                new ArrayList<>(Collections.singletonList(ANOTHER_DOC_ID)),
                expectedFinalDecisionAndReasonPdf}
        };
    }

    private static Object[] theOverviewTabFieldIsNullScenario() {
        AsylumCase asylumCase = buildCaseWithPopulatedFieldForGivenDoc(null);

        return new Object[] {
            new Object[] {
                asylumCase,
                new AsylumCase(),
                new ArrayList<>(Collections.singletonList(DOC_ID)),
                null}
        };
    }

    private static Object[] documentIsUpdatedScenario() {
        AsylumCase asylumCase = buildCaseWithPopulatedFieldForGivenDoc(buildDocumentGivenDocId());
        asylumCase.write(FINAL_DECISION_AND_REASONS_DOCUMENTS, Collections.singletonList(buildIdValue()));
        Document expectedFinalDecisionAndReasonPdf = buildDocumentGivenDocId();

        return new Object[] {
            new Object[] {
                asylumCase,
                new AsylumCase(),
                new ArrayList<>(Collections.singletonList(DOC_ID)),
                expectedFinalDecisionAndReasonPdf}
        };
    }

    private static AsylumCase buildCaseWithPopulatedFieldForGivenDoc(Document document) {
        AsylumCase asylumCase = new AsylumCase();
        asylumCase.write(AsylumCaseFieldDefinition.FINAL_DECISION_AND_REASONS_PDF, document);
        return asylumCase;
    }

    private static Document buildDocumentGivenDocId() {
        return new Document("http://dm-store/" + DOC_ID, "", "");
    }

    private static DocumentWithMetadata buildDocWithMeta() {
        return new DocumentWithMetadata(buildDocumentGivenDocId(), "", "", DocumentTag.NONE);
    }

    private static IdValue<DocumentWithMetadata> buildIdValue() {
        return new IdValue<>("1", buildDocWithMeta());
    }
}
