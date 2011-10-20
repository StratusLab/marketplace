<xsl:stylesheet version="1.0"
            xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:slterms="http://mp.stratuslab.eu/slterms#"
            xmlns:slreq="http://mp.stratuslab.eu/slreq#"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:ex="http://example.org/">
            
            
<xsl:output method="html" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template match="rdf:Description">
<xsl:choose>
   <xsl:when test="string(dcterms:title)">
    <h1><xsl:value-of select="dcterms:title"/></h1>
   </xsl:when>

   <xsl:otherwise>
    <h1><xsl:value-of select="dcterms:identifier"/></h1>
   </xsl:otherwise>
</xsl:choose>
<p><xsl:value-of select="dcterms:description"/></p>
<table class="inline">

<xsl:if test="string(slterms:deprecated)">
<tr><td><b>deprecated: </b></td><td><b><font color="red"><xsl:value-of select="slterms:deprecated"/></font></b></td></tr>
</xsl:if>

<tr><td><b>type:</b></td><td><xsl:value-of select="dcterms:type"/></td></tr>

<xsl:if test="string(slterms:kind)">
<tr><td><b>kind: </b></td><td><xsl:value-of select="slterms:kind"/></td></tr>
</xsl:if>

<xsl:if test="string(dcterms:format)">
<tr><td><b>format: </b></td><td><xsl:value-of select="dcterms:format"/></td></tr>
</xsl:if>

<tr>
<td><b>endorser:</b></td>
<td><a><xsl:attribute name="href">../../../endorsers/<xsl:value-of select="slreq:endorsement/slreq:endorser/slreq:email"/></xsl:attribute><xsl:value-of select="slreq:endorsement/slreq:endorser/slreq:email"/></a>
</td>
</tr>

<xsl:if test="string(slterms:os)">
<tr>
<td><b>os: </b>
</td>
<td><xsl:value-of select="slterms:os"/>
<xsl:if test="string(slterms:os-version)">
<xsl:text> v</xsl:text><xsl:value-of select="slterms:os-version"/>
</xsl:if>
<xsl:if test="string(slterms:os-arch)">
<xsl:text> </xsl:text><xsl:value-of select="slterms:os-arch"/>
</xsl:if>
</td>
</tr>
</xsl:if>

<xsl:if test="string(slterms:version)">
<tr><td><b>version: </b></td><td><xsl:value-of select="slterms:version"/></td></tr>
</xsl:if>

<xsl:if test="string(dcterms:created)">
<tr><td><b>created: </b></td><td><xsl:value-of select="dcterms:created"/></td></tr>
</xsl:if>
<xsl:if test="string(dcterms:valid)">
<tr><td><b>valid: </b></td><td><xsl:value-of select="dcterms:valid"/></td></tr>
</xsl:if>

<xsl:if test="string(slterms:hypervisor)">
<tr><td><b>hypervisor: </b></td><td><xsl:value-of select="slterms:hypervisor"/></td></tr>
</xsl:if>

<xsl:if test="string(dcterms:publisher)">
<tr><td><b>publisher: </b></td><td><xsl:value-of select="dcterms:publisher"/></td></tr>
</xsl:if>

<xsl:if test="string(slreq:bytes)">
<tr><td><b>bytes: </b></td><td><xsl:value-of select="slreq:bytes"/></td></tr>
</xsl:if>

<tr>
<td><b>checksum:</b></td>
<td>
<xsl:for-each select="slreq:checksum">
<xsl:value-of select="slreq:algorithm"/><xsl:text> </xsl:text><xsl:value-of select="slreq:value"/><br/>
</xsl:for-each>
</td>
</tr>

<xsl:if test="string(slterms:location)">
<tr><td><b>location:</b></td>
<td>
<xsl:for-each select="slterms:location">
<a><xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute><xsl:value-of select="."/></a><br/>
</xsl:for-each>
</td>
</tr>
</xsl:if>

</table>
</xsl:template>
</xsl:stylesheet>

