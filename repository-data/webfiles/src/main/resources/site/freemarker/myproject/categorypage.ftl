<#include "../include/imports.ftl">

<div>
  <h1>Category Page</h1>
  <#if document?? && document.title?has_content>
    <h4>${document.title}</h4>
  </#if>
  <#if document?? && document.overview?has_content>
    ${document.overview}
  </#if>
  <#if folderLinks??>
      <#list folderLinks as folderName, folderLink>
          <a href="${folderLink}">${folderName}</a>
      </#list>
  </#if>
</div>