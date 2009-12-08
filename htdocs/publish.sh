#! /bin/sh

# change this
sfuser=jeromerobert
rsync -avz build/site/ $sfuser,jcae@web.sf.net:htdocs/

