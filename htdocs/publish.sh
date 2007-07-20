#! /bin/sh

# change this
sfuser=$USER

rsync -avz build/site/ $sfuser@shell.sf.net:/home/groups/j/jc/jcae/htdocs/

