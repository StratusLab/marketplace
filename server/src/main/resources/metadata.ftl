<html>
<head>
<meta http-equiv="CONTENT-TYPE" CONTENT="text/html; charset=utf-8">
 <title>  StratusLab :: Combining Grid and Cloud Technologies  </title>
 <SCRIPT SRC="http://cagnode56.cs.tcd.ie/images/include/mktree.js" LANGUAGE="JavaScript"></SCRIPT>
 <LINK REL="stylesheet" HREF="http://cagnode56.cs.tcd.ie/images/include/mktree.css">
 <link rel="stylesheet" media="screen" type="text/css" href="http://cagnode56.cs.tcd.ie/images/include/mmkanso/css/design.css" />
 <link rel="stylesheet" media="screen" type="text/css" href="http://cagnode56.cs.tcd.ie/images/include/mmkanso/css/menu.css" />
 <link rel="stylesheet" media="screen" type="text/css" href="http://cagnode56.cs.tcd.ie/images/include/mmkanso/css/gallery.css" />
</head>
<body>
<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>
<div class="Content">
<h1>${title}</h1>

<ul class="mktree" id="tree1">
  <#list content?keys as identifier>
    <li>${identifier}
    <ul>
      <#assign endorsers = content[identifier]> 
      <#list endorsers?keys as email>
      <li>${email}
        <ul>
          <li><a href="${identifier}/${email}/${endorsers[email]}">${endorsers[email]}</a></li>
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
