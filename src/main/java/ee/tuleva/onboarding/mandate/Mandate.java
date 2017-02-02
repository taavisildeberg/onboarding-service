package ee.tuleva.onboarding.mandate;

import com.fasterxml.jackson.annotation.JsonView;
import ee.tuleva.domain.fund.Fund;
import ee.tuleva.domain.fund.FundView;
import ee.tuleva.onboarding.user.User;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.Instant;

@Data
@Entity
@Table(name = "mandate")
public class Mandate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(FundView.SkipFundManager.class)
    private Long id;

    @ManyToOne
    User user;

    @ManyToOne
    Fund targetFund;

    @NotNull
    @Past
    private Instant createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = Instant.now();
    }

    @NotNull
    byte[] mandate;
}
