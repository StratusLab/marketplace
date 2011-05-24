<html>
  <#include "header.ftl">
  <#include "breadcrumbs.ftl">

<body>

<script type="text/javascript">
function removeLast() {
   var aFieldset=document.queryform.getElementsByTagName('fieldset');
   var count=aFieldset.length;
   if(count>1) {
      aFieldset[0].parentNode.removeChild(aFieldset[count-1]);
   }
}

function addMore() {
   var aFieldset=document.queryform.getElementsByTagName('fieldset');
   var count=aFieldset.length;
   var cloned=aFieldset[0].cloneNode(true); // fieldset containing select, input & input
   var parent=aFieldset[0].parentNode; // form
   // re-name
   var oSel=parent.getElementsByTagName('select')[0];
   cloned.getElementsByTagName('select')[0].name=oSel.name+count;
   var aInput=parent.getElementsByTagName('input');
   var cInput=cloned.getElementsByTagName('input');
   cInput[0].name=aInput[0].name+count;
   count++;
   // insert clone
   var oDiv=parent.getElementsByTagName('div')[0];
   parent.insertBefore(cloned, oDiv);
}
</script>
<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>
<div class="Content">
<h1>${title}</h1>

<form action="/search" method="get" name="queryform">
<fieldset>
<table>
<tr>
<td>
<select name="qname">
<option value="identifier">identifier</option>
<option value="isReplacedBy">isReplacedBy</option>
<option value="replaces">replaces</option>
<option value="isVersionOf">isVersionOf</option>
<option value="valid">valid</option>
<option value="title">title</option>
<option value="description">description</option>
<option value="type">type</option>
<option value="creator">creator</option>
<option value="created">created</option>
<option value="publisher">publisher</option>
<option value="format">format</option>
<option value="email">email</option>
<option value="bytes">bytes</option>
<option value="checksum">checksum</option>
<option value="replaces">replaces</option>
<option value="subject">subject</option>
<option value="issuer">issuer</option>
<option value="location">location</option>
<option value="serial-number">serial-number</option>
<option value="version">version</option>
<option value="hypervisor">hypervisor</option>
<option value="os-arch">os-arch</option>
<option value="os-version">os-version</option>
<option value="os">os</option>
</select>
</td>
<td>
<input type="value" name="value">
</td>
</tr>
</table>
</fieldset>
<div>
<button type="button" onclick="addMore();">+</button><button type="button" onclick="removeLast();">-</button>
<button type="submit">search</button>
</div>
</form>

<br/>

<ul class="mktree" id="tree1">
  <#list content?keys as identifier>
    <li>${identifier}
    <ul>
      <#assign endorsers = content[identifier]> 
      <#list endorsers?keys as email>
      <li>${email}
        <ul>
          <li><a href="/metadata/${identifier}/${email}/${endorsers[email]}">${endorsers[email]}</a></li>
        </ul>
      </li>
      </#list>
    </ul>
    </li>
  </#list>
</ul>

<div class="Footer">
                StratusLab is co-funded by the European Community's<br/>Seventh Framework Programme (Capacities)<br/>Grant Agreement INFSO-RI-261552            </div>
</div>

</body>
</html>
