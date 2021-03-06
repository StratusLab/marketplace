<xsl:stylesheet version="1.0"
            xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:slterms="http://mp.stratuslab.eu/slterms#"
            xmlns:slreq="http://mp.stratuslab.eu/slreq#"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:ex="http://example.org/">
            
            
<xsl:output method="html" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template name="text_wrapper">
<xsl:param name="text"/>
<xsl:param name="width" select="20"/> 

<xsl:if test="string-length($text)">
  <xsl:value-of select="substring($text, 1, $width)"/><br/>
  <xsl:call-template name="text_wrapper">
    <xsl:with-param name="text"
     select="substring($text, $width + 1)"/>
    <xsl:with-param name="width" select="$width"/>
  </xsl:call-template>
</xsl:if>
</xsl:template>

<xsl:template match="rdf:Description">
<xsl:choose>
   <xsl:when test="string(dcterms:title)">
    <h1><xsl:value-of select="dcterms:title"/>

    <xsl:if test="string(dcterms:alternative)">
      (<xsl:value-of select="dcterms:alternative"/>)
    </xsl:if>
    </h1>
   
   </xsl:when>

   <xsl:otherwise>
    <xsl:if test="string(dcterms:alternative)">
      <h1><xsl:value-of select="dcterms:alternative"/></h1>
    </xsl:if>
   </xsl:otherwise>
</xsl:choose>

<table class="inline" style="margin-bottom:15px;">
<tr class="spaceunder">
<xsl:variable name="os">
  <xsl:value-of select="translate(slterms:os,'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
</xsl:variable>
<xsl:choose>
  <xsl:when test="contains($os, 'centos')">
    <td><img src="img/centos.jpg" align="left"/></td>
  </xsl:when>
  <xsl:when test="contains($os, 'ubuntu')">
    <td><img src="img/ubuntu.png" align="left"/></td>
  </xsl:when>
  <xsl:when test="contains($os, 'scientificlinux') or contains($os, 'sl')">
    <td><img src="img/scientific.png" align="left"/></td>
  </xsl:when>  
  <xsl:when test="contains($os, 'fedora')">
    <td><img src="img/fedora.jpg" align="left"/></td>
  </xsl:when>
  <xsl:otherwise>
    <td><img src="img/stratuslab.jpg" align="left"/></td>
  </xsl:otherwise>
</xsl:choose>
<td><xsl:value-of select="dcterms:description"/></td>
</tr>
<tr><td colspan="2">Endorsed by <xsl:value-of select="slreq:endorsement/slreq:endorser/slreq:email"/> at <xsl:value-of select="slreq:endorsement/dcterms:created"/></td></tr>
</table>

<table class="metadata">
<xsl:if test="string(slterms:deprecated)">
<tr><td><b>Deprecated </b></td><td><b><font color="red"><xsl:value-of select="slterms:deprecated"/></font></b></td></tr>
</xsl:if>

<tr><td><b>Identifier </b></td><td><xsl:value-of select="dcterms:identifier"/></td></tr>

<tr><td><b>Type </b></td><td><xsl:value-of select="dcterms:type"/></td></tr>

<xsl:if test="string(slterms:kind)">
<tr><td><b>Kind </b></td><td><xsl:value-of select="slterms:kind"/></td></tr>
</xsl:if>

<xsl:if test="string(dcterms:format)">
<tr><td><b>Format </b></td><td><xsl:value-of select="dcterms:format"/></td></tr>
</xsl:if>

<xsl:if test="string(slterms:os)">
<tr>
<td><b>OS </b>
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
<tr><td><b>Version </b></td><td><xsl:value-of select="slterms:version"/></td></tr>
</xsl:if>

<xsl:if test="string(dcterms:created)">
<tr><td><b>Created </b></td><td><xsl:value-of select="dcterms:created"/></td></tr>
</xsl:if>
<xsl:if test="string(dcterms:valid)">
<tr><td><b>Valid </b></td><td><xsl:value-of select="dcterms:valid"/></td></tr>
</xsl:if>

<xsl:if test="string(slterms:hypervisor)">
<tr><td><b>Hypervisor </b></td><td><xsl:value-of select="slterms:hypervisor"/></td></tr>
</xsl:if>

<xsl:if test="string(dcterms:publisher)">
<tr><td><b>Publisher </b></td><td><xsl:value-of select="dcterms:publisher"/></td></tr>
</xsl:if>

<xsl:if test="string(slreq:bytes)">
<tr><td><b>Bytes </b></td><td><xsl:value-of select="slreq:bytes"/></td></tr>
</xsl:if>

<tr>
<td><b>Checksum </b></td>
<td>
<table border="0">
<xsl:for-each select="slreq:checksum">
<tr>
  <td><b><xsl:value-of select="slreq:algorithm"/></b></td>
  <td>
    <xsl:call-template name="text_wrapper">
        <xsl:with-param name="text" select="slreq:value"/>
        <xsl:with-param name="width" select="40"/>
    </xsl:call-template>
  </td>
</tr>
</xsl:for-each>
</table>
</td>
</tr>

<xsl:if test="string(slterms:location)">
<tr><td><b>Location </b></td>
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

