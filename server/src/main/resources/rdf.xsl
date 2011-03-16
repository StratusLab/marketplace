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
<h1><xsl:value-of select="dcterms:title"/></h1>
<p><xsl:value-of select="dcterms:description"/></p>
<table>
<tr><td><b>type:</b></td><td><xsl:value-of select="dcterms:type"/></td></tr>
<tr>
<td><b>checksum:</b></td>
<td>
<xsl:for-each select="slreq:checksum">
<xsl:value-of select="slreq:algorithm"/><xsl:text> </xsl:text><xsl:value-of select="slreq:value"/><br/>
</xsl:for-each>
</td>
</tr>
<tr>
<td><b>endorser:</b></td>
<td><a><xsl:attribute name="href">/endorsers/<xsl:value-of select="slreq:endorsement/slreq:endorser/slreq:email"/></xsl:attribute><xsl:value-of select="slreq:endorsement/slreq:endorser/slreq:email"/></a>
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

