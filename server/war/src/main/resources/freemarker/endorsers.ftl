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

      <table border=0>
          <#list content as emails>
              <tr>
              <td><a href=endorsers/${emails.email}>${emails.email}</a></td>
              </tr>
           </#list>
      </table>

  </div>
</div>

</body>
</html>
