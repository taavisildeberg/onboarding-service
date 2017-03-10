package ee.tuleva.onboarding.mandate;

import com.codeborne.security.mobileid.MobileIDSession;
import com.codeborne.security.mobileid.MobileIdSignatureSession;
import com.fasterxml.jackson.annotation.JsonView;
import ee.tuleva.onboarding.auth.mobileid.MobileIdSessionStore;
import ee.tuleva.onboarding.auth.mobileid.MobileIdSignatureSessionStore;
import ee.tuleva.onboarding.mandate.exception.MandateNotFoundException;
import ee.tuleva.onboarding.user.User;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MandateController {

    private final MandateRepository mandateRepository;
    private final MandateService mandateService;
    private final MobileIdSignatureSessionStore mobileIdSignatureSessionStore;
    private final MobileIdSessionStore mobileIdSessionStore;

    @ApiOperation(value = "Create a mandate")
    @RequestMapping(method = POST, value = "/mandates")
    @JsonView(MandateView.Default.class)
    public Mandate create(@ApiIgnore @AuthenticationPrincipal User user,
                          @Valid @RequestBody CreateMandateCommand createMandateCommand,
                          @ApiIgnore @Valid Errors errors) {

        if (errors.hasErrors()) {
            throw new ErrorsValidationException(errors);
        }

        return mandateService.save(user, createMandateCommand);
    }

    @ApiOperation(value = "Sign mandate")
    @RequestMapping(method = PUT, value = "/mandates/{id}/signature")
    public MandateSignatureResponse startSign(@PathVariable("id") Long mandateId,
                                              @ApiIgnore @AuthenticationPrincipal User user) {

        MobileIDSession loginSession = mobileIdSessionStore.get()
                .orElseThrow(() -> new IllegalStateException("No mobile id session found"));

        MobileIdSignatureSession signatureSession = mandateService.sign(mandateId, user, loginSession.phoneNumber);

        mobileIdSignatureSessionStore.save(new MandateSignatureSession(signatureSession.sessCode, signatureSession.challenge));

        return MandateSignatureResponse.builder()
                .mobileIdChallengeCode(signatureSession.challenge)
                .build();
    }

    @ApiOperation(value = "Is mandate successfully signed")
    @RequestMapping(method = GET, value = "/mandates/{id}/signature")
    public MandateSignatureStatusResponse getSignatureStatus(@PathVariable("id") Long mandateId,
                                                             @ApiIgnore @AuthenticationPrincipal User user) {

        MandateSignatureSession session = mobileIdSignatureSessionStore.get();
        String status = mandateService.getSignatureStatus(mandateId, session);

        return MandateSignatureStatusResponse.builder()
                .statusCode(status)
                .build();
    }

    @ApiOperation(value = "Get mandate file")
    @RequestMapping(method = GET, value = "/mandates/{id}/file")
    public void getSignatureStatus(@PathVariable("id") Long mandateId,
                                   @ApiIgnore @AuthenticationPrincipal User user,
                                   HttpServletResponse response) throws IOException {

        Mandate mandate = mandateRepository.findByIdAndUser(mandateId, user);

        if(mandate == null) {
            throw new MandateNotFoundException();
        } else {
            response.addHeader("Content-Disposition", "attachment; filename=avaldus.bdoc");

            IOUtils.copy(new ByteArrayInputStream(mandate.getMandate()), response.getOutputStream());
            response.flushBuffer();
        }

    }

}
