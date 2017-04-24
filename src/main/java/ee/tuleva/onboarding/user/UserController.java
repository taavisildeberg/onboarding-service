package ee.tuleva.onboarding.user;

import ee.tuleva.onboarding.auth.principal.AuthenticatedPerson;
import ee.tuleva.onboarding.error.ValidationErrorsException;
import ee.tuleva.onboarding.user.command.UpdateUserCommand;
import ee.tuleva.onboarding.user.exception.SaveUserException;
import ee.tuleva.onboarding.user.preferences.CsdUserPreferencesService;
import ee.tuleva.onboarding.user.preferences.UserPreferences;
import ee.tuleva.onboarding.user.response.UserResponse;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1")
@AllArgsConstructor
public class UserController {

	private final CsdUserPreferencesService preferencesService;
	private final UserRepository userRepository;

	@ApiOperation(value = "Get info about the current user")
	@GetMapping("/me")
	public UserResponse me(@ApiIgnore @AuthenticationPrincipal AuthenticatedPerson authenticatedPerson) {
		return UserResponse.fromAuthenticatedPerson(authenticatedPerson);
	}

	@ApiOperation(value = "Update the current user")
	@PatchMapping("/me")
	public UserResponse patchMe(@ApiIgnore @AuthenticationPrincipal AuthenticatedPerson authenticatedPerson,
								@Valid @RequestBody UpdateUserCommand cmd,
								@ApiIgnore Errors errors) throws ValidationErrorsException {

		if (errors != null && errors.hasErrors()) {
			throw new ValidationErrorsException(errors);
		}

		User user = userRepository.findByPersonalCode(authenticatedPerson.getPersonalCode());
		user.setEmail(cmd.getEmail());
		user.setPhoneNumber(cmd.getPhoneNumber());

		try {
			user = userRepository.save(user);
		} catch(DataIntegrityViolationException e) {
			throw new SaveUserException("Error saving user", e);
		}
		return UserResponse.fromUser(user);
	}

	@ApiOperation(value = "Get info about the current user preferences from CSD")
	@GetMapping("/preferences")
	public UserPreferences getPreferences(@ApiIgnore @AuthenticationPrincipal AuthenticatedPerson authenticatedPerson) {
		return preferencesService.getPreferences(authenticatedPerson.getPersonalCode());
	}

}