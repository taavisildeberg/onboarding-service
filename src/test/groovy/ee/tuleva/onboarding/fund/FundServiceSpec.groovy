package ee.tuleva.onboarding.fund

import ee.tuleva.onboarding.fund.statistics.PensionFundStatistics
import ee.tuleva.onboarding.fund.statistics.PensionFundStatisticsService
import spock.lang.Specification

import static ee.tuleva.onboarding.mandate.MandateFixture.sampleFunds
import static java.util.Collections.singletonList
import static java.util.stream.Collectors.toList

class FundServiceSpec extends Specification {

	def fundRepository = Mock(FundRepository)
	def pensionFundStatisticsService = Mock(PensionFundStatisticsService)
	
	def service = new FundService(fundRepository, pensionFundStatisticsService)

	def "can get funds with along statistics"() {
		given:
		String fundManagerName = "Tuleva"
		Iterable<Fund> funds = sampleFunds().stream().filter( { f -> f.fundManager.name == fundManagerName}).collect(toList())
		fundRepository.findByFundManagerNameIgnoreCase(fundManagerName) >> funds
		def tulevaFund = funds.first()
		def volume = 1_000_000
		def nav = 1.64
		pensionFundStatisticsService.getCachedStatistics() >> singletonList(new PensionFundStatistics(tulevaFund.isin, volume, nav))

		when:
		def response = service.getFunds(Optional.of(fundManagerName))
		
		then:
		response.size() == 1
		def fund = response.first()
		fund.isin == tulevaFund.isin
		fund.volume == volume
		fund.nav == nav
	}
}
