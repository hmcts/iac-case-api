package uk.gov.hmcts.reform.iacaseapi.domain.entities.ref;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.LegRepAddressUk;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ProfessionalUser;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganisationEntityResponse {

    private String organisationIdentifier;
    private String name;
    private String status;
    private boolean sraRegulated;
    private ProfessionalUser superUser;
    private List<String> paymentAccount;
    private List<LegRepAddressUk> contactInformation;

    public String getOrganisationIdentifier() {
        requireNonNull(organisationIdentifier);
        return organisationIdentifier;
    }

    public List<String> getPaymentAccount() {
        return paymentAccount;
    }

    public List<LegRepAddressUk> getContactInformation() {
        return contactInformation;
    }
}
