package pl.tomaszdziurko.jvm_bloggers.view.login

import org.apache.wicket.authroles.authorization.strategies.role.Roles
import org.apache.wicket.util.tester.FormTester
import pl.tomaszdziurko.jvm_bloggers.MockSpringContextAwareSpecification
import pl.tomaszdziurko.jvm_bloggers.blog_posts.domain.BlogPostRepository
import pl.tomaszdziurko.jvm_bloggers.mailing.LogMailSenderPostAction
import pl.tomaszdziurko.jvm_bloggers.utils.NowProvider
import pl.tomaszdziurko.jvm_bloggers.view.admin.AdminDashboardPage
import pl.tomaszdziurko.jvm_bloggers.view.login.attack.BruteForceLoginAttackDetector

class LoginPageSpec extends MockSpringContextAwareSpecification {

    LogMailSenderPostAction logMailPostAction = new LogMailSenderPostAction()

    UserAuthenticator userAuthenticator = Stub()

    Roles adminRoles = Stub() {
        hasRole(Roles.ADMIN) >> true
    }

    Roles notAdminRoles = Stub() {
        hasRole(Roles.ADMIN) >> false
    }

    def void setupContext() {
        NowProvider nowProvider = new NowProvider()
        addBean(nowProvider)
        addBean(userAuthenticator)
        addBean(new BruteForceLoginAttackDetector())
        addBean(Mock(BlogPostRepository))
    }

    def "Should redirect to Admin Dashboard after successful login"() {
        given:
            String login = "login"
            String pass = "pass"
            userAuthenticator.getRolesForUser(login, pass) >> adminRoles
        when:
            tester.startPage(LoginPage.class)
            FormTester formTester = tester.newFormTester(LoginPage.LOGIN_FORM_ID)
            formTester.setValue(LoginPage.LOGIN_FIELD_ID, login)
            formTester.setValue(LoginPage.PASSWORD_FIELD_ID, pass)
            formTester.submit(LoginPage.FORM_SUBMIT_ID)
        then:
            tester.assertNoErrorMessage()
            tester.assertRenderedPage(AdminDashboardPage.class)
    }

    def "Should reject incorrect password"() {
        given:
            userAuthenticator.getRolesForUser(_, _) >> notAdminRoles
        when:
            tester.startPage(LoginPage.class)
            FormTester formTester = tester.newFormTester(LoginPage.LOGIN_FORM_ID)
            formTester.setValue(LoginPage.LOGIN_FIELD_ID, "Any User")
            formTester.setValue(LoginPage.PASSWORD_FIELD_ID, "incorrect password")
            formTester.submit(LoginPage.FORM_SUBMIT_ID)
        then:
            tester.assertErrorMessages("Incorrect login or password")
            tester.assertRenderedPage(LoginPage.class)
    }

    def "Should not try to login after brute force attack was detected"() {
        given:
            userAuthenticator.getRolesForUser(_, _) >> notAdminRoles
        when:
            (1..4).each {
                tester.startPage(LoginPage.class)
                FormTester formTester = tester.newFormTester(LoginPage.LOGIN_FORM_ID)
                formTester.setValue(LoginPage.LOGIN_FIELD_ID, "Any User")
                formTester.setValue(LoginPage.PASSWORD_FIELD_ID, "incorrect password")
                formTester.submit(LoginPage.FORM_SUBMIT_ID)
            }
        then:
            tester.assertErrorMessages("Incorrect login or password [BruteForce attack was detected]")
            tester.assertRenderedPage(LoginPage.class)
    }
}