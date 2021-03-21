<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("loginTitle", "Information Form")}
    <#elseif section = "header">
        ${msg("loginTitleHtml", "Please fill the missing fields")}
    <#elseif section = "form">
        <form id="kc-update-institute-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="username" class="${properties.kcLabelClass!}">Username</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="username" name="username" value="${(user.username!'')}"
                            class="${properties.kcInputClass!}" />
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="email" class="${properties.kcLabelClass!}">Email</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if user.email?has_content>
                        <input type="text" id="email" name="email" value="${(user.email!'')}"
                                class="${properties.kcInputClass!}"
                                    readonly
                                    />
                    <#else>
                        <input type="text" id="email" name="email" value="${(user.email!'')}"
                                class="${properties.kcInputClass!}" />
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="firstName" class="${properties.kcLabelClass!}">Firstname</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if user.firstName?has_content>
                        <input type="text" id="firstName" name="firstName" value="${(user.firstName!'')}"
                                class="${properties.kcInputClass!}"
                                        readonly
                                        />
                    <#else>
                        <input type="text" id="firstName" name="firstName" value="${(user.firstName!'')}"
                                class="${properties.kcInputClass!}" />
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="lastName" class="${properties.kcLabelClass!}">Lastname</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if user.lastName?has_content>
                        <input type="text" id="lastName" name="lastName" value="${(user.lastName!'')}"
                                class="${properties.kcInputClass!}"
                                        readonly
                                        />
                    <#else>
                        <input type="text" id="lastName" name="lastName" value="${(user.lastName!'')}"
                                class="${properties.kcInputClass!}" />
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="user.attributes.institute" class="${properties.kcLabelClass!}">Institute</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if user.attributes.institute?has_content>
                        <input type="text" id="user.attributes.institute" name="user.attributes.institute" value="${(user.attributes.institute!'')}"
                                class="${properties.kcInputClass!}"
                                        readonly
                                        />
                    <#else>
                        <input type="text" id="user.attributes.institute" name="user.attributes.institute" value="${(user.attributes.institute!'')}"
                                class="${properties.kcInputClass!}" />
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
