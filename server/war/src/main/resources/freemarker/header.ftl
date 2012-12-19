<head>
  <meta http-equiv="CONTENT-TYPE" CONTENT="text/html; charset=utf-8">

  <title>${title!}</title>

  <base href="${baseurl}"> 

  <script type="text/javascript" language="javascript" src="/js/jquery.min.js"></script>
  <script type="text/javascript" language="javascript" src="/js/jquery.dataTables.min.js"></script>
  <script type="text/javascript" language="javascript" src="/js/jquery.datatable.listbox.plugin.js"></script>
  <script type="text/javascript" language="javascript" src="/js/jquery.dataTables.pipelining.js"></script>

  <script type="text/javascript">
        $(function() {
            var offset = $("#sidebar").offset();
            var topPadding = 15;
            $(window).scroll(function() {
                if ($(window).scrollTop() > offset.top) {
                    $("#sidebar").stop().animate({
                        marginTop: $(window).scrollTop() - offset.top + topPadding
                    });
                } else {
                    $("#sidebar").stop().animate({
                        marginTop: 0
                    });
                };
            });
        });
  </script>

  <link rel="shortcut icon" href="css/favicon.ico"/>
  <link rel="stylesheet" type="text/css" href="css/service.css"/>

</head>
