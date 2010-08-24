#! /bin/sh
cp applauncher.cpp jcaelauncher.cpp
patch jcaelauncher.cpp launcher.patch
i586-mingw32msvc-windres -o jcae.res -Ocoff jcae.rc
i586-mingw32msvc-g++ -mwindows app.cpp jcaelauncher.cpp ../../ide/launcher/windows/nblauncher.cpp ../../o.n.bootstrap/launcher/windows/utilsfuncs.cpp jcae.res -o jcae.exe
