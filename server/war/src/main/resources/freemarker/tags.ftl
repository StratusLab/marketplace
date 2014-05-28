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

     <table cellpadding="0" cellspacing="0" border="0" class="display" id="endorsertags">
	  <thead>
	      <tr>
                  <th>Tag</th>
              </tr>
          </thead>
          <tbody>
            <#list content as tags>
              <tr>
              <td><a href=metadata/${email}?tag="${tags.tag}">${tags.tag}</a></td>
              </tr>
             </#list>
           </tbody>
      </table>

      <script type="text/javascript" language="javascript" src="js/tags.js"></script>

</div>
</div>

</body>
</html>
