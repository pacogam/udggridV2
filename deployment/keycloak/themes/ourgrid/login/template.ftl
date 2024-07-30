<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayGraphic=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}">
<head>
	<meta charset="utf-8">
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
	<title>${msg("applicationName")}</title>
    <link rel="icon" type="image/svg" href="${url.resourcesPath}/img/ourgrid_star_favicon.svg"/>
    <link type=text/css rel="stylesheet" href="${url.resourcesPath}/css/materialize.min.css" media="screen,projection"/>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css"/>
	<script type="text/javascript" src="${url.resourcesPath}/js/materialize.min.js"></script>
</head>

<body>
<div id="outer-wrapper">
    <div id="wrapper" style="position: relative;">
        <#if displayGraphic>
            <div id="background-top" class="fadein-image" style="--og-duration-medium-entry: 0.6s; position: absolute; top: 0; width: 100%; max-height: 22vh; padding: 16px; display: flex; justify-content: center;">
                <img src="${url.resourcesPath}/img/green-dots-heading.svg" style="width: 100%; max-width: 600px; object-fit: cover; object-position: bottom;">
            </div>
            <div id="background-bottom" class="fadein-image" style="--og-duration-medium-entry: 0.6s; position: absolute; bottom: 0; width: 100%; max-height: 22vh; padding: 16px; display: flex; justify-content: center;">
                <img src="${url.resourcesPath}/img/green-dots-heading.svg" style="width: 100%; max-width: 600px; object-fit: cover; object-position: bottom; rotate: 180deg;">
            </div>
        </#if>
        <div class="row" style="position: relative; margin-bottom: 0;">
            <div class="col s10 m6 l4 offset-s1 offset-m3 offset-l4">
                <div class="row">
                    <div class="row">
                        <div class="col s12 center fadein-animation" style="--og-duration-medium-entry: 0.45s;">
                            <div id="header-wrapper">
                                <span class="text-title">OurGrid</span>
                            </div>
                        </div>
                    </div>

                    <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
                        <div class="col m12" id="kc-locale">
                            <div id="kc-locale-dropdown">
                                <a href="#" id="kc-current-locale-link">${locale.current}</a>
                                <ul>
                                    <#list locale.supported as l>
                                        <li><a href="${l.url}">${l.label}</a></li>
                                    </#list>
                                </ul>
                            </div>
                        </div>
                    </#if>

                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="section">
                            <div class="card-panel">
                                <#if message.type=='success' ><i class="material-icons green-text">check_circle</i><span
                                        class="green-text">${kcSanitize(message.summary)?no_esc}</span></#if>
                                <#if message.type=='warning' ><i class="material-icons orange-text">warning</i><span
                                        class="orange-text">${kcSanitize(message.summary)?no_esc}</span></#if>
                                <#if message.type=='error' ><i class="material-icons red-text">error</i><span
                                        class="red-text">${kcSanitize(message.summary)?no_esc}</span></#if>
                                <#if message.type=='info' ><i class="material-icons blue-text">info</i><span
                                        class="blue-text">${kcSanitize(message.summary)?no_esc}</span></#if>
                            </div>
                        </div>
                    </#if>

                    <div class="col s12">
                        <#nested "form">
                    </div>
                </div>
                <div class="row">
                    <div class="col s12">
                        <#if displayInfo>
                            <#nested "info">
                        </#if>
                    </div>
                </div>
            </div>

        </div>
    </div>
</div>
</body>
</html>
</#macro>
