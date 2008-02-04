#! /bin/sh
files="
	branding/core/core.jar/org/netbeans/core/startup/Bundle.properties \
	branding/modules/org-netbeans-core.jar/org/netbeans/core/ui/Bundle.properties \
	branding/modules/org-netbeans-core-windows.jar/org/netbeans/core/windows/view/ui/Bundle.properties \
	core/manifest.mf \
	make-dist-Linux.sh \
	make-dist-Win32.sh \
	nbproject/project.properties \
	occjava-nb/manifest.mf"

for file in $files; do
	sed -i "s/@JCAE_VERSION@/$1/g" $file
done

