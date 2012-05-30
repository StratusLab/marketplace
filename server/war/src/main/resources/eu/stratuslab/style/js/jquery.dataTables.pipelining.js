var oCache = {
                iCacheLower: -1
};

function fnSetKey( aoData, sKey, mValue )
{
        for ( var i=0, iLen=aoData.length ; i<iLen ; i++ )
        {
                if ( aoData[i].name == sKey )
                {
                        aoData[i].value = mValue;
                }
        }
}

function fnGetKey( aoData, sKey )
{
        for ( var i=0, iLen=aoData.length ; i<iLen ; i++ )
        {
                if ( aoData[i].name == sKey )
                {
                        return aoData[i].value;
                }
        }
        return null;
}

function fnDataTablesPipeline ( sSource, aoData, fnCallback ) {
        var iPipe = 5; /* Adjust the pipe size */

        var bNeedServer = false;
        var sEcho = fnGetKey(aoData, "sEcho");
        var iRequestStart = fnGetKey(aoData, "iDisplayStart");
        var iRequestLength = fnGetKey(aoData, "iDisplayLength");
        var iRequestEnd = iRequestStart + iRequestLength;
        oCache.iDisplayStart = iRequestStart;

        /* outside pipeline? */
        if ( oCache.iCacheLower < 0 || iRequestStart < oCache.iCacheLower || iRequestEnd > oCache.iCacheUpper )
        {
                bNeedServer = true;
        }

        /* sorting etc changed? */
        if ( oCache.lastRequest && !bNeedServer )
        {
                for( var i=0, iLen=aoData.length ; i<iLen ; i++ )
                {
                        if ( aoData[i].name != "iDisplayStart"
                                && aoData[i].name != "iDisplayLength"
                                        && aoData[i].name != "sEcho" )
                        {
                                if ( aoData[i].value != oCache.lastRequest[i].value )
                                {
                                        bNeedServer = true;
                                        break;
                                }
                        }
                }
        }

        /* Store the request for checking next time around */
        oCache.lastRequest = aoData.slice();

        if ( bNeedServer )
        {
                if ( iRequestStart < oCache.iCacheLower )
                {
                        iRequestStart = iRequestStart - (iRequestLength*(iPipe-1));
                        if ( iRequestStart < 0 )
                        {
                                iRequestStart = 0;
                        }
                }

                oCache.iCacheLower = iRequestStart;
                oCache.iCacheUpper = iRequestStart + (iRequestLength * iPipe);
                oCache.iDisplayLength = fnGetKey( aoData, "iDisplayLength" );
                fnSetKey( aoData, "iDisplayStart", iRequestStart );
                fnSetKey( aoData, "iDisplayLength", iRequestLength*iPipe );

                var re = /[?&]([^=]+)(?:=([^&]*))?/g;
                var matchInfo;

                while(matchInfo = re.exec(window.location.search))
                {
                        aoData.push( {"name": matchInfo[1], "value": matchInfo[2]} );
                }

                $.ajax( {
                        "dataType": 'json',
                        "type": "POST",
                        "url": sSource,
                        error: function (jqXHR, textStatus, errorThrown) {
                                alert(jqXHR, textStatus, errorThrown);
                        },
                        "data": aoData,
                        "success": function (json) {
                                var message = json.rMsg;
                                if(message.indexOf("ERROR") != -1){
                                        alert(message);
                                }
                                oTable.fnSettings().oLanguage.sZeroRecords = json.rMsg;
                                oTable.fnSettings().oLanguage.sEmptyTable = json.rMsg;

                                /* Callback processing */
                                oCache.lastJson = jQuery.extend(true, {}, json);

                                if ( oCache.iCacheLower != oCache.iDisplayStart )
                                {
                                        json.aaData.splice( 0, oCache.iDisplayStart-oCache.iCacheLower );
                                }
                                json.aaData.splice( oCache.iDisplayLength, json.aaData.length );

                                fnCallback(json);
                        }
                });
        }
        else
        {
                json = jQuery.extend(true, {}, oCache.lastJson);
                json.sEcho = sEcho; /* Update the echo for each response */
                json.aaData.splice( 0, iRequestStart-oCache.iCacheLower );
                json.aaData.splice( iRequestLength, json.aaData.length );
                fnCallback(json);
                return;
        }
}
