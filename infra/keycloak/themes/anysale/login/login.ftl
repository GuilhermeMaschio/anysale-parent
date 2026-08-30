<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username', 'password') displayInfo=false; section>
  <#if section = "header">
    Entre para continuar
  <#elseif section = "form">
    <p class="anysale-login-intro">Use sua conta corporativa para acessar a operação comercial.</p>
    <form id="kc-form-login" class="pf-v5-c-form" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
      <div class="pf-v5-c-form__group">
        <label for="username" class="pf-v5-c-form__label"><span class="pf-v5-c-form__label-text">E-mail</span></label>
        <span class="pf-v5-c-form-control <#if messagesPerField.existsError('username', 'password')>pf-m-error</#if>">
          <input id="username" name="username" value="${(login.username!'')}" type="text" autocomplete="username" autofocus aria-invalid="<#if messagesPerField.existsError('username', 'password')>true<#else>false</#if>" />
        </span>
        <#if messagesPerField.existsError('username')><span class="pf-v5-c-helper-text pf-m-error"><span class="pf-v5-c-helper-text__item-text">${kcSanitize(messagesPerField.get('username'))?no_esc}</span></span></#if>
      </div>

      <div class="pf-v5-c-form__group">
        <label for="password" class="pf-v5-c-form__label"><span class="pf-v5-c-form__label-text">Senha</span></label>
        <span class="pf-v5-c-form-control <#if messagesPerField.existsError('username', 'password')>pf-m-error</#if>">
          <input id="password" name="password" type="password" autocomplete="current-password" aria-invalid="<#if messagesPerField.existsError('username', 'password')>true<#else>false</#if>" />
        </span>
        <#if messagesPerField.existsError('password')><span class="pf-v5-c-helper-text pf-m-error"><span class="pf-v5-c-helper-text__item-text">${kcSanitize(messagesPerField.get('password'))?no_esc}</span></span></#if>
      </div>

      <input type="hidden" id="id-hidden-input" name="credentialId" />
      <button class="pf-v5-c-button pf-m-primary pf-m-block" name="login" id="kc-login" type="submit">Entrar</button>
    </form>
  </#if>
</@layout.registrationLayout>
