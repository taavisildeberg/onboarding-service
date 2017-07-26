package ee.tuleva.onboarding.account;

import ee.eesti.xtee6.kpr.PensionAccountBalanceResponseType;
import ee.eesti.xtee6.kpr.PensionAccountBalanceType;
import ee.eesti.xtee6.kpr.PersonalSelectionResponseType;
import ee.tuleva.onboarding.auth.principal.Person;
import ee.tuleva.onboarding.fund.Fund;
import ee.tuleva.onboarding.fund.FundRepository;
import ee.tuleva.onboarding.kpr.KPRClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountStatementService {

    private final KPRClient kprClient;
    private final FundRepository fundRepository;
    private final KPRUnitsOsakudToFundBalanceConverter kprUnitsOsakudToFundBalanceConverter;

    private final String NO_SECOND_PILLAR_FAULT_CODE = "40351";

    private final String ACCOUNT_STATEMENT_CACHE_NAME = "accountStatement";

    @Cacheable(value=ACCOUNT_STATEMENT_CACHE_NAME, key="#person.personalCode")
    public List<FundBalance> getMyPensionAccountStatement(Person person) {
        log.info("Getting pension account statement for {} {}", person.getFirstName(), person.getLastName());
        List<FundBalance> fundBalances = convertXRoadResponse(getPensionAccountBalance(person));

        return getActiveFundName(person)
                .map(activeFundName -> handleActiveFundBalance(fundBalances, activeFundName))
                .orElse(fundBalances);
    }

    @CacheEvict(value=ACCOUNT_STATEMENT_CACHE_NAME, key="#person.personalCode")
    public void clearCache(Person person) {
        log.info("Clearning exchanges cache for {} {}",
                person.getFirstName(), person.getLastName());
    }

    private PensionAccountBalanceResponseType getPensionAccountBalance(Person person) {
        PensionAccountBalanceType request = new PensionAccountBalanceType();
        request.setBalanceDate(null);
        PensionAccountBalanceResponseType response = null;
        try {
            response = kprClient.pensionAccountBalance(request, person.getPersonalCode());
        } catch (Exception e) {
            throw new PensionRegistryAccountStatementConnectionException();
        }
        return response;
    }

    private List<FundBalance> convertXRoadResponse(PensionAccountBalanceResponseType response) {
        if (response.getFaultCode() != null &&
                response.getFaultCode().equals(NO_SECOND_PILLAR_FAULT_CODE)) {
            log.info("Person didn't have second pillar pension funds");
            return Arrays.asList();
        }

        try {
            return response.getUnits().getBalance().stream()
                    .map(kprUnitsOsakudToFundBalanceConverter::convert)
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            log.error("Unknown response conversion error. Fault code {}", response.getFaultCode());
            return Arrays.asList();
        }
    }

    private Optional<String> getActiveFundName(Person person) {
        PersonalSelectionResponseType csdPersonalSelection;
        try {
            csdPersonalSelection = kprClient.personalSelection(person.getPersonalCode());
        } catch (Exception e) {
            throw new PensionRegistryAccountStatementConnectionException();
        }

        if (csdPersonalSelection.getFaultCode() != null &&
                csdPersonalSelection.getFaultCode().equals(NO_SECOND_PILLAR_FAULT_CODE)) {
            log.info("Person didn't have second pillar personal data.");
            return Optional.empty();
        }

        String activeFundName = csdPersonalSelection.getPensionAccount().getSecurityName();
        log.info("Active fund name is {}", activeFundName);
        return Optional.of(activeFundName);
    }

    private List<FundBalance> handleActiveFundBalance(List<FundBalance> fundBalances, String activeFundName) {
        if (fundBalances.size() == 0) {
            return Arrays.asList();
        }

        fundBalances.stream().forEach( fb-> {
            log.info("Having fund {} with isin {}",
                    fb.getFund().getName(), fb.getFund().getIsin());
        });

        Optional<FundBalance> activeFundBalance = fundBalances.stream()
                .filter(fb -> fb.getFund().getName().equalsIgnoreCase(activeFundName))
                .findFirst();

        activeFundBalance.ifPresent( fb -> {
            fb.setActiveContributions(true);
            log.info("Setting active fund {}", fb.getFund().getName());
        });
        if(!activeFundBalance.isPresent()) {
            log.info("Didn't find active fund {} from the list, creating one.", activeFundName);
            fundBalances.add(constructActiveFundBalance(activeFundName));
        }
        return fundBalances;
    }

    private FundBalance constructActiveFundBalance(String activeFundName) {
        FundBalance activeFundBalance = FundBalance.builder()
                .value(BigDecimal.ZERO)
                .currency("EUR")
                .activeContributions(true)
                .build();

        Fund activeFund = fundRepository.findByNameIgnoreCase(activeFundName);
        if (activeFund != null) {
            activeFundBalance.setFund(activeFund);
        } else {
            log.error("Fund with name not found {}", activeFundName);
            activeFundBalance.setFund(Fund.builder().name(activeFundName).build());
        }

        log.info("Constructed active fund for {} with isin {}",
                activeFund.getName(), activeFund.getIsin());

        return activeFundBalance;
    }

}
