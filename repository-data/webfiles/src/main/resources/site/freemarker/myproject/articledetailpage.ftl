<#include "../include/imports.ftl">

<div>
  <h1>Article Detail Page</h1>
  <#if document?? && document.title?has_content>
    <h4>${document.title}</h4>
  </#if>
</div>