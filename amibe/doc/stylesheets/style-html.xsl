<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<!-- Import HTML base stylesheet -->
<xsl:import href="file:///usr/share/sgml/docbook/stylesheet/xsl/nwalsh/html/chunk.xsl"/>

<!-- Put correct encoding into the header -->
<xsl:param name="chunker.output.encoding" select="'UTF-8'"/>

<!-- Generate readable HTML code -->
<xsl:param name="chunker.output.indent" select="'yes'"/>

<!-- Write sections into seperate pages -->
<xsl:param name="chunk.first.sections" select="1"/>

<!-- Write sections in the ToC -->
<xsl:param name="chunk.section.depth" select="2"/>

<!-- Fix section numbering -->
<xsl:param name="section.autolabel">1</xsl:param>
<xsl:param name="section.label.includes.component.label">1</xsl:param>

</xsl:stylesheet>
