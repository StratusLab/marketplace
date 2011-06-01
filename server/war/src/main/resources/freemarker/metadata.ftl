<html>
  <#include "header.ftl">
  <#include "breadcrumbs.ftl">

<body>

<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>
<div class="Content">
<h1>${title}</h1>

<br/>

<table id="form_with_details" class="display">
    <thead>
        <tr>
            <th>os</th>
            <th>os-version</th>
            <th>arch</th>
            <th>email</th>
            <th>identifier</th>
            <th>date</th>
            <th>description</th>
            <th>location</th>
        </tr>
    </thead>
    <tbody>
        <#list content?keys as identifier>
            <#assign endorsers = content[identifier]>
            <#list endorsers?keys as email>
               <#assign created = endorsers[email]>
               <#list created?keys as date>
                    <#assign data = created[date]>
                    <tr>
                        <td>${data.os}</td>
                        <td>${data.osversion}</td>
                        <td>${data.arch}</td>
                        <td>${email}</td>
                        <td>${identifier}</td>
                        <td>${date}</td>
                        <td>${data.location}</td>
                        <td>${data.description}</td>
                    </tr>
                </#list>
             </#list>
        </#list>
     </tbody>
     <tfoot>
         <tr>
             <th></th>
             <th><input type="text" name="search_os" value="Search os" class="search_init" /></th>
	     <th><input type="text" name="search_osversion" value="Search os version" class="search_init" /></th>
	     <th><input type="text" name="search_arch" value="Search architecture" class="search_init" /></th>
             <th><input type="text" name="search_email" value="Search email" class="search_init" /></th>
             <th><input type="text" name="search_identifier" value="Search identifier" class="search_init" /></th>
             <th><input type="text" name="search_date" value="Search date" class="search_init" /></th>
             <th><input type="text" name="search_location" value="Search location" class="search_init" /></th>
             <th><input type="text" name="search_description" value="Search description" class="search_init" /></th>
         </tr>
      </tfoot>
</table>

<#include "tabledetails-js.ftl">

</div>
</div>

</body>
</html>
