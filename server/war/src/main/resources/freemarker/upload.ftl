<html>
  <#include "header.ftl">
  <#include "breadcrumbs.ftl">
<body>
<div class="Page">
  <div class="Header">
     <div class="Banner" />
  </div>

  <div class="Content">

    <h1>${title}</h1>

    <form action="../metadata/" enctype="multipart/form-data" method="POST">
      <table>
        <tbody>
          <tr>
            <td><input type="file" name="Metadata File" size="40"></td>
          </tr>
          <tr>
            <td><input type="submit" value="Submit"></td>
          </tr>
        </tbody>
      </table>
    </form>
  </div>

  <div class="Footer">
    StratusLab is co-funded by the European Community's<br/>
    Seventh Framework Programme (Capacities)<br/>
    Grant Agreement INFSO-RI-261552
  </div>

</div>

</body>
</html>
