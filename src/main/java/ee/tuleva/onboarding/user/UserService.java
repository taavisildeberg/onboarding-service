package ee.tuleva.onboarding.user;

import ee.tuleva.onboarding.notification.mailchimp.MailChimpService;
import ee.tuleva.onboarding.user.exception.UserAlreadyAMemberException;
import ee.tuleva.onboarding.user.member.Member;
import ee.tuleva.onboarding.user.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.apache.commons.lang3.text.WordUtils.capitalizeFully;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final MemberRepository memberRepository;
  private final MailChimpService mailChimpService;

  public User getById(Long userId) {
    return userRepository.findOne(userId);
  }

  // TODO: return Optional<User> instead
  @Nullable
  public User findByPersonalCode(String personalCode) {
    return userRepository.findByPersonalCode(personalCode).orElse(null);
  }

  public User createNewUser(User user) {
    return userRepository.save(user);
  }

  public User updateUser(String personalCode, String email, String phoneNumber) {
    User user = findByPersonalCode(personalCode);
    user.setEmail(email);
    user.setPhoneNumber(phoneNumber);
    return updateUser(user);
  }

  public User registerAsMember(Long userId) {
    User user = userRepository.findOne(userId);

    if (user.getMember().isPresent()) {
      throw new UserAlreadyAMemberException("User is already a member!");
    }

    Member newMember = Member.builder()
      .user(user)
      .memberNumber(memberRepository.getNextMemberNumber())
      .build();

    log.info("Registering user as new member #{}: {}", newMember.getMemberNumber(), user);

    user.setMember(newMember);
    return updateUser(user);
  }

  public boolean isAMember(Long userId) {
    Optional<User> user = Optional.ofNullable(userRepository.findOne(userId));
    return user.map(u -> u.getMember().isPresent()).orElse(false);
  }

  private User updateUser(User user) {
    User savedUser = userRepository.save(user);
    mailChimpService.createOrUpdateMember(savedUser);
    return savedUser;
  }

  public User updateNameIfMissing(User user, String fullName) {
    if (!user.hasName()) {
      String firstName = substringBeforeLast(fullName, " ");
      String lastName = substringAfterLast(fullName, " ");
      user.setFirstName(capitalizeFully(firstName));
      user.setLastName(capitalizeFully(lastName));
      return userRepository.save(user);
    }
    return user;
  }

  public User createOrUpdateUser(String personalCode, String email, String phoneNumber) {
    if (isAMember(personalCode, email)) {
      throw new UserAlreadyAMemberException("This user is already a member");
    }

    clearPersonalCodeCollision(personalCode);

    User user = userRepository.findByEmail(email)
      .map(u -> {
        u.setPersonalCode(personalCode);
        u.setEmail(email);
        u.setPhoneNumber(phoneNumber);
        return u;
      }).orElse(User.builder()
        .personalCode(personalCode)
        .email(email)
        .phoneNumber(phoneNumber)
        .active(true)
        .build());

    return userRepository.save(user);
  }

  private void clearPersonalCodeCollision(String personalCode) {
    userRepository.findByPersonalCode(personalCode).ifPresent(user -> {
      user.setPersonalCode(null);
      userRepository.save(user);
    });
  }

  private boolean isAMember(String personalCode, String email) {
    return isAMemberByPersonalCode(personalCode) || isAMemberByEmail(email);
  }

  private boolean isAMemberByPersonalCode(String personalCode) {
    Optional<User> user = userRepository.findByPersonalCode(personalCode);
    return user.map(u -> u.getMember().isPresent()).orElse(false);
  }

  private boolean isAMemberByEmail(String email) {
    Optional<User> user = userRepository.findByEmail(email);
    return user.map(u -> u.getMember().isPresent()).orElse(false);
  }

}
