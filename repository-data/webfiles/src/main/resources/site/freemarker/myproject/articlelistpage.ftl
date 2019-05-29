<#include "../include/imports.ftl">

<div>
  <h1>Article List Page</h1>
  <#if document?? && document.title?has_content>
    <h4>${document.title}</h4>
  </#if>
  <#if document?? && document.overview?has_content>
    ${document.overview}
  </#if>
  <#if articleLinks??>
      <#list articleLinks as articleName, articleLink>
          <div>
              <a href="${articleLink}">${articleName}</a>
          </div>
      </#list>
  </#if>
</div>