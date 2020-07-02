package uk.gov.hmcts.reform.iacaseapi.infrastructure.workallocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartResponse {
    private String id;

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "StartResponse{" +
                "id='" + id + '\'' +
                '}';
    }
}
