package ee.tuleva.onboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Predicates.or;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
@RestController
public class SwaggerConfiguration {

    @RequestMapping(value = "/swagger", method = GET)
    public void encyclopedia(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui.html");
    }

    @Bean
    public Docket accountIdentityApi() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
                .apis(RequestHandlerSelectors.any())
                .paths(or(regex("/auth.*"), regex("/v1/.*")))
            .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Tuleva onboarding service")
            .description("")
            .contact(new Contact("Tuleva", "https://github.com/TulevaEE", "tonu.pekk@tuleva.ee"))
            .version("1.0")
            .build();
    }
}
