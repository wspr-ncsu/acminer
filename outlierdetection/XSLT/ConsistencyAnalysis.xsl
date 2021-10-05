<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:eg="http://sag.org" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:ext="http://saxon.sag.org">

<xsl:template name="html_header">
	<head>
		<meta name="viewport" content="width=device-width, initial-scale=1"/>
		<style type="text/css">
		h2 {
		  text-align: center;
		  background-color: gray;
		  font-size: 1.7em;
		}
		
		h3{
		  margin-left: 40px;
		  font-size: 1.35em;
		}
		
		h4{
		  margin-left: 25px;
		  font-size: 1.15em;
		  margin-top: 0;
		  margin-bottom: 0;
		}
		
		.toggle-box {
		  display: none;
		}

		.toggle-box + label {
		  cursor: pointer;
		  display: block;
		  font-weight: bold;
		  font-size: 1.25em;
		  line-height: 21px;
		  margin-bottom: 5px;
		  margin-left: 10px;
		}

		.toggle-box + label + div {
		  display: none;
		  margin-bottom: 5px;
		  margin-left: 20px;
		}

		.toggle-box:checked + label + div {
		  display: block;
		}

		.toggle-box + label:before {
		  background-color: #4F5150;
		  -webkit-border-radius: 10px;
		  -moz-border-radius: 10px;
		  border-radius: 10px;
		  color: #FFFFFF;
		  content: "+";
		  display: block;
		  float: left;
		  font-weight: bold;
		  height: 20px;
		  line-height: 20px;
		  margin-right: 5px;
		  text-align: center;
		  width: 20px;
		}

		.toggle-box:checked + label:before {
		  content: "\2212";
		}
		
		#stub-group {
		  background-color: LightGray;
		}
		
		#des_info {
		   display: inline;
		   font-weight: bold;
		   word-wrap: none;
		}
		
		body {
		  font-family: "Linux Libertine", Courier;
		  font-size: 10pt;
		  background-color: #FFFFFF;
		  word-wrap: break-word;
		  width: auto;
		}
		
		xmp { 
		  white-space: pre-wrap; 
		  word-wrap: break-word;
		  display: inline;
		  font-family: Courier;
		  font-size: 13pt;
		}
		
		table {
		  margin-left: 35px;
		  table-layout: fixed;
		  width: 96%
		}
		
		th {
		  white-space: nowrap;
		  font-weight: bold;
		  text-align: left;
		  vertical-align: top;
		  width: 0%
		}
		
		td {
		  white-space: normal;
		  word-wrap: break-word;
		  text-align: left;
		  vertical-align: top;
		}
		
		tr:nth-child(even) {
		  background-color: #f2f2f2;
		}
		
		tr:nth-child(odd) {
		  background-color: #c5c5c5;
		}

		</style>
	</head>
</xsl:template>

<xsl:template name="gen" match="entrypoint">
	<html>
		<xsl:call-template name="html_header"/>
		<body>
			<div id="stub-group">
				<xsl:variable name="hyperlink"><xsl:value-of select="@href" /></xsl:variable>
				<h2>Entrypoint <xsl:value-of select="@id" />: <xsl:value-of select="@name" /> </h2>
				<xsl:apply-templates mode="rule" select="/"/>
				<br />
			</div>
		</body>
	</html>
</xsl:template>

<xsl:template mode="rule" match="rule">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Rule (</xsl:text>
		<xsl:text>Confidence = </xsl:text>
		<xsl:value-of select="@confidence" />
		<xsl:text>, Jaccard = </xsl:text>
		<xsl:value-of select="@jaccard" />
		<xsl:text>, Support = </xsl:text>
		<xsl:value-of select="@support" />
		<xsl:text>, Support % = </xsl:text>
		<xsl:value-of select="@psupport" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="antecedent" select="antecedent"/>
		<xsl:apply-templates mode="consequent" select="consequent"/>
		<xsl:apply-templates mode="missing_antecedent" select="missing_antecedent"/>
		<xsl:apply-templates mode="other_entrypoints" select="other_entrypoints"/>
		<xsl:apply-templates mode="aosp_diff" select="aosp_diff"/>
	</div>
</xsl:template>

<xsl:template mode="aosp_diff" match="aosp_diff">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>AOSP DIFF</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="rule_diff" select="rule_diff"/>
	</div>
</xsl:template>

<xsl:template mode="rule_diff" match="rule_diff">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Rule (</xsl:text>
		<xsl:text>ID = </xsl:text>
		<xsl:value-of select="@rule_id" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="diff_same" select="diff_same"/>
		<xsl:apply-templates mode="diff_neg" select="diff_neg"/>
		<xsl:apply-templates mode="diff_pos" select="diff_pos"/>
	</div>
</xsl:template>

<xsl:template mode="diff_same" match="diff_same">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Similar (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>

<xsl:template mode="diff_neg" match="diff_neg">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>AOSP contains but not Vendor (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>


<xsl:template mode="diff_pos" match="diff_pos">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Vendor contains but not AOSP (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>


<xsl:template mode="antecedent" match="antecedent">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Authorization Checks Occurring in All Entry Points (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>

<xsl:template mode="consequent" match="consequent">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Recommended Authorization Checks from Supporting Entry Points (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>

<xsl:template mode="missing_antecedent" match="missing_antecedent">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Remaining Authorization Checks of Target Entry Point (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>

<xsl:template mode="alternative_checks" match="alternative_checks">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Alternative Checks (stripMethodArgs(X) in RHS and stripMethodArgs(X) in Entrypoint: Hints inconsistency in method args) (</xsl:text>
		<xsl:value-of select="count(item)" />
		<xsl:text>)</xsl:text>
	</label>
	<div>
		<xsl:apply-templates mode="item" select="item"/>
	</div>
</xsl:template>

<xsl:template mode="item" match="item">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:value-of select="@name" />
	</label>
	<div>
		<table border="0">
			<xsl:for-each select="source_methods/method">
				<tr><th></th><td><xsl:value-of select="@name" /></td></tr>
			</xsl:for-each>
		</table>
	</div>
</xsl:template>

<xsl:template mode="other_entrypoints" match="other_entrypoints">
	<xsl:variable name="uniq_id" select="ext:randomUUID()" as="xs:string" />
	<input class="toggle-box" id="#{$uniq_id}" type="checkbox" />
	<label for="#{$uniq_id}" style="font-size: 1.35em;">
		<xsl:text>Entrypoints</xsl:text>
	</label>
	<div>
		<table border="0">
			<xsl:for-each select="epoint">
				<tr><th></th><td><xsl:value-of select="@name" /></td></tr>
			</xsl:for-each>
		</table>
	</div>
</xsl:template>


</xsl:stylesheet>
