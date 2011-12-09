<html>
  <#include "header.ftl">
<body>
<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>

  <#include "breadcrumbs.ftl">

  <div class="Content">
      <h1>${title}</h1>

      <table id="form_with_details" class="display">
    <thead>
        <tr>
            <th>Endorser</th>
        </tr>
    </thead>
    <tbody>
         <#list content as emails>
              <tr>
		<td><a href=metadata/${emails.email}>${emails.name}</a></td>
                <td>${emails.subject}</td>
                <td>${emails.issuer}</td>
                <td>${emails.email}</td>
              </tr>
           </#list>
     </tbody>
     <tfoot>
         <tr>
             <th></th>
             <th><input type="text" name="search_name" value="Search name" class="search_init" /></th>
             <th><input type="text" name="search_subject" value="Search subject" class="search_init" /></th>
             <th><input type="text" name="search_issuer" value="Search issuer" class="search_init" /></th>
             <th><input type="text" name="search_email" value="Search email" class="search_init" /></th>
         </tr>
      </tfoot>
</table>

<script type="text/javascript" language="javascript" src="/js/endorsers.js"></script>
  
</div>
</div>

</body>
</html>
