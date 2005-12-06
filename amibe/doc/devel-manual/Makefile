
DOC = devel-manual

all: html
html:
	xsltproc --xinclude ../stylesheets/style-html.xsl $(DOC).xml

valid:
	xmllint --valid --noout $(DOC).xml

clean:
	rm -f *.html

.PHONY:
	clean all
