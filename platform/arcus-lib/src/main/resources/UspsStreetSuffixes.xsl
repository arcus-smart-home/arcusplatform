<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
[
   <xsl:apply-templates select="//tr[count(td)=3]" />
]
</xsl:template>

<xsl:template match="tr[count(td)=3]">

<xsl:variable name="code" select="normalize-space(td[3]/p/strong/a)" />
<xsl:variable name="name" select="normalize-space(td[1]/p/a)" />
<xsl:variable name="firstVariant" select="normalize-space(td[2]/p/a)" />
<xsl:variable name="followingSiblingCount" select="td[1]/@rowspan - 1" />

{
   "code":"<xsl:value-of select="$code" />",
   "name":"<xsl:value-of select="$name" />",
   "variants":
   [
      "<xsl:value-of select="$firstVariant" />"
      <xsl:for-each select="following-sibling::tr[position() &lt;= $followingSiblingCount]">
      , "<xsl:value-of select="normalize-space(td[1]/p/a)" />"
      </xsl:for-each>
   ]
},

</xsl:template>

</xsl:stylesheet>
