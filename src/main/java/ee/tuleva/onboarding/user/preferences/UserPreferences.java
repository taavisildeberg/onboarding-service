package ee.tuleva.onboarding.user.preferences;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * User preferences from CSD KPR registry.
 */
@Getter
@Setter
@Builder
public class UserPreferences {

  public enum ContactPreferenceType {E, P} // E - email, P - postal

  private ContactPreferenceType contactPreference;

  private String districtCode;

  private String addressRow1;

  private String addressRow2;

  private String addressRow3;

  private String postalIndex;

  private String country;

  public enum LanguagePreferenceType {EST, RUS, ENG}

  private LanguagePreferenceType languagePreference;

  private String noticeNeeded; // boolean { 'Y', 'N' }

  public static UserPreferences defaultUserPreferences() {
    return builder()
        .addressRow1("Tuleva, Telliskivi 60")
        .addressRow2("TALLINN")
        .addressRow3("TALLINN")
        .country("EE")
        .postalIndex("10412")
        .districtCode("0784")
        .contactPreference(UserPreferences.ContactPreferenceType.valueOf("E"))
        .languagePreference(UserPreferences.LanguagePreferenceType.valueOf("EST"))
        .noticeNeeded("Y")
        .build();
  }

}
