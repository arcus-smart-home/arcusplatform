<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


<xsl:template match="/">

<xsl:apply-templates select="//table[//th = 'New Tag']//td[1]" />

<xsl:apply-templates select="//table[//th = 'New Tag']//td[3]" />

<xsl:apply-templates select="//table[//th = 'New Tag']//td[4]" />

</xsl:template>


<xsl:template match="td">

<xsl:value-of select="normalize-space()" /><xsl:text>&#xa;</xsl:text>

</xsl:template>


</xsl:stylesheet>
