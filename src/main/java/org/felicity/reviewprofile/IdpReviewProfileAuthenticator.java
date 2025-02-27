/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.felicity.reviewprofile;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.LegacyUserProfileProviderFactory;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.profile.DefaultUserProfileContext;
import org.keycloak.userprofile.profile.representations.AttributeUserProfile;
import org.keycloak.userprofile.utils.UserUpdateHelper;
import org.keycloak.userprofile.validation.UserProfileValidationResult;
import org.keycloak.forms.login.freemarker.model.ProfileBean;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.authentication.requiredactions.util.UserUpdateProfileContext;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:groverkss@gmail.com">Kunwar Shaanjeet Singh Grover</a>
 */
public class IdpReviewProfileAuthenticator extends org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator {

    private static final Logger logger = Logger.getLogger(IdpReviewProfileAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected LoginFormsProvider setReviewAttributes (LoginFormsProvider res, SerializedBrokeredIdentityContext userCtxSeralized, BrokeredIdentityContext brokerContext, MultivaluedMap<String, String> formData) {
        UpdateProfileContext userCtx = (UpdateProfileContext) userCtxSeralized;
        res.setAttribute("user", new ProfileBean(userCtx, formData));
        return res;
    }

    protected boolean checkUsernameExists(AuthenticationFlowContext context, String username) {
        UserModel existingUser = context.getSession().users().getUserByUsername(username, context.getRealm());
        return existingUser != null;
    }

    protected boolean checkEmailExists(AuthenticationFlowContext context, String email) {
        UserModel existingUser = context.getSession().users().getUserByEmail(email, context.getRealm());
        return existingUser != null;
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext userCtx, BrokeredIdentityContext brokerContext) {
        IdentityProviderModel idpConfig = brokerContext.getIdpConfig();

        if (requiresUpdateProfilePage(context, userCtx, brokerContext)) {

            logger.debugf("Identity provider '%s' requires update profile action for broker user '%s'.", idpConfig.getAlias(), userCtx.getUsername());

            // No formData for first render. The profile is rendered from userCtx
            LoginFormsProvider challengeIntermediate = context.form();

            Response challengeResponse = setReviewAttributes(challengeIntermediate, userCtx, brokerContext, null)
                .createForm("institute-form.ftl");

            context.challenge(challengeResponse);
        } else {
            // Not required to update profile. Marked success
            context.success();
        }
    }

    protected boolean requiresUpdateProfilePage(AuthenticationFlowContext context, SerializedBrokeredIdentityContext userCtx, BrokeredIdentityContext brokerContext) {
        String enforceUpdateProfile = context.getAuthenticationSession().getAuthNote(ENFORCE_UPDATE_PROFILE);
        if (Boolean.parseBoolean(enforceUpdateProfile)) {
            return true;
        }

        String updateProfileFirstLogin;
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null || !authenticatorConfig.getConfig().containsKey(IdpReviewProfileAuthenticatorFactory.UPDATE_PROFILE_ON_FIRST_LOGIN)) {
            updateProfileFirstLogin = IdentityProviderRepresentation.UPFLM_MISSING;
        } else {
            updateProfileFirstLogin = authenticatorConfig.getConfig().get(IdpReviewProfileAuthenticatorFactory.UPDATE_PROFILE_ON_FIRST_LOGIN);
        }

        List<String> institute = userCtx.getAttribute("institute");

        boolean isInstituteSet = institute != null && !institute.isEmpty();

        boolean uniqueEmail = !checkEmailExists(context, brokerContext.getEmail());
        boolean uniqueUsername = !checkUsernameExists(context, brokerContext.getUsername());

        RealmModel realm = context.getRealm();
        return IdentityProviderRepresentation.UPFLM_ON.equals(updateProfileFirstLogin)
            || (IdentityProviderRepresentation.UPFLM_MISSING.equals(updateProfileFirstLogin) && (!Validation.validateUserMandatoryFields(realm, userCtx)) || !isInstituteSet || !uniqueUsername || !uniqueEmail);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext userCtx, BrokeredIdentityContext brokerContext) {
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_PROFILE);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        for(String str: formData.keySet()) {
            logger.infof("Form Data: %s : %s", str, formData.getFirst(str));
        }

        UserProfileProvider profileProvider = context.getSession().getProvider(UserProfileProvider.class, LegacyUserProfileProviderFactory.PROVIDER_ID);
        AttributeUserProfile updatedProfile = AttributeFormDataProcessor.toUserProfile(formData);

        // Set institute in updated profile
        if (formData.getFirst("user.attributes.institute") != null) {
            updatedProfile.getAttributes().setSingleAttribute("institute", formData.getFirst("user.attributes.institute"));
        }

        // Get institute and alias from updated file
        String instituteAttribute = updatedProfile.getAttributes().getFirstAttribute("institute");

        String oldEmail = userCtx.getEmail();
        String newEmail = updatedProfile.getAttributes().getFirstAttribute(UserModel.EMAIL);

        UserProfileValidationResult result = profileProvider.validate(DefaultUserProfileContext.forIdpReview(userCtx), updatedProfile);
        List<FormMessage> errors = Validation.getFormErrorsFromValidation(result);

        // Add institute errors
        if (instituteAttribute == null || instituteAttribute.isEmpty()) {
            errors.add(new FormMessage("institute", "Please specify an Institute"));
        }

        // Get username and email specified 
        String formEmail = updatedProfile.getAttributes().getFirstAttribute(UserModel.EMAIL);
        String formUsername = updatedProfile.getAttributes().getFirstAttribute(UserModel.USERNAME);

        // Check if already exists and set errors

        if (checkUsernameExists(context, formUsername)) {
            errors.add(new FormMessage("username", "Username already exists. Please use something else"));
        }

        if (checkEmailExists(context, formEmail)) {
            errors.add(new FormMessage("email", "Email already exists. Please login using your existing account."));
        }

        if (errors != null && !errors.isEmpty()) {
            LoginFormsProvider challengeIntermediate = context.form()
                .setErrors(errors);

            Response challenge = setReviewAttributes(challengeIntermediate, userCtx, brokerContext, formData)
                .createForm("institute-form.ftl");

            context.challenge(challenge);
            return;
        }

        UserUpdateHelper.updateIdpReview(context.getRealm(), new UserModelDelegate(null) {
            @Override
            public Map<String, List<String>> getAttributes() {
                return userCtx.getAttributes();
            }

            @Override
            public Stream<String> getAttributeStream(String name) {
                return userCtx.getAttribute(name).stream();
            }

            @Override
            public void setAttribute(String name, List<String> values) {
                userCtx.setAttribute(name, values);
            }

            @Override
            public void removeAttribute(String name) {
                userCtx.getAttributes().remove(name);
            }
        }, updatedProfile);

        userCtx.saveToAuthenticationSession(context.getAuthenticationSession(), BROKERED_CONTEXT_NOTE);

        logger.debugf("Profile updated successfully after first authentication with identity provider '%s' for broker user '%s'.", brokerContext.getIdpConfig().getAlias(), userCtx.getUsername());

        if (result.hasAttributeChanged(UserModel.EMAIL)) {
            context.getAuthenticationSession().setAuthNote(UPDATE_PROFILE_EMAIL_CHANGED, "true");
            event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, newEmail).success();
        }
        event.detail(Details.UPDATED_EMAIL, newEmail);

        // Ensure page is always shown when user later returns to it - for example with form "back" button
        context.getAuthenticationSession().setAuthNote(ENFORCE_UPDATE_PROFILE, "true");

        context.success();
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
