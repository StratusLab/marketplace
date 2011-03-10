<xsl:stylesheet version="1.0"
            xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:slterms="http://mp.stratuslab.eu/slterms#"
            xmlns:slreq="http://mp.stratuslab.eu/slreq#"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:ex="http://example.org/">
            
            
<xsl:output method="html" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:key name="entityByType" match="rdf:Description" use="substring-after(rdf:type/@rdf:resource, 'type/em/e/')"/>

<xsl:template match="/*">
<xsl:apply-templates select="
rdf:Description[
generate-id() = generate-id(key('entityByType', substring-after(rdf:type/@rdf:resource, 'type/em/e/')))
]"/>
</xsl:template>

<xsl:template match="rdf:Description">
<div class="block">
<xsl:variable name="entityType">
<xsl:call-template name="substring-after-last">
<xsl:with-param name="input" select="rdf:type/@rdf:resource"/>
 <xsl:with-param name="substr" select="'/'"/>
 </xsl:call-template>
 </xsl:variable>
  <div class="header {$entityType}">
 <xsl:value-of select="$entityType"/>
</div>
<xsl:apply-templates select="key('entityByType', $entityType)" mode="inner-content"/>
</div>
</xsl:template>

<xsl:template match="rdf:Description" mode="inner-content">
<div class="image">
   <h1><xsl:value-of select="dcterms:title"/></h1>
   <p><xsl:value-of select="dcterms:description"/></p>
   <b>os: </b><xsl:value-of select="slterms:os"/><xsl:text> v</xsl:text><xsl:value-of select="slterms:os-version"/><xsl:text> </xsl:text><xsl:value-of select="slterms:os-arch"/><br/>
   <b>checksum: </b><xsl:value-of select="slreq:checksum/slreq:algorithm"/><xsl:text> </xsl:text><xsl:value-of select="slreq:checksum/slreq:value"/><br/>
   <b>type: </b><xsl:value-of select="dcterms:type"/><br/>
   <b>valid: </b><xsl:value-of select="dcterms:valid"/><br/>
   <b>publisher: </b><xsl:value-of select="dcterms:publisher"/><br/>
   <b>version: </b><xsl:value-of select="slterms:version"/><br/>
   <b>endorser: </b><xsl:value-of select="slreq:endorsement/slreq:endorser/slreq:email"/><br/>
   <b>location: </b><a><xsl:attribute name="href"><xsl:value-of select="slterms:location"/></xsl:attribute><xsl:value-of select="slterms:location"/></a><br/>
</div>
</xsl:template>

<xsl:template name="substring-after-last">
<xsl:param name="input"/>
<xsl:param name="substr"/>
<xsl:variable name="temp" select="substring-after($input,$substr)"/>
<xsl:choose>
<xsl:when test="$substr and contains($temp,$substr)">
<xsl:call-template name="substring-after-last">
<xsl:with-param name="input"  select="$temp" />
<xsl:with-param name="substr" select="$substr" />
</xsl:call-template>
</xsl:when>
<xsl:otherwise>
<xsl:value-of select="$temp"/>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

</xsl:stylesheet>

