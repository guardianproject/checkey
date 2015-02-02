
Checkey is a utility for getting information about the APKs that are installed
on your device. Starting with a list of all of the apps that you have
installed on your device, it will show you the APK signature with a single
touch, and provides links to virustotal.com and androidobservatory.org to
easily access the profiles of that APK. It will also let you export the
signing certificate and generate ApkSignaturePin pin files for use with the
TrustedIntents library.


Building
========

Building Checkey is quite straightforward since it is a simple, pure Java
project with very few external dependencies.

  git clone https://github.com/guardianproject/checkey
  cd checkey
  ./setup-ant
  ant clean debug

Once that has completed, you can install it however you would normally install
an .apk file.  You will find the .apk in the bin/ folder.  An easy way to
install it via the terminal is to run:

  adb install bin/Checkey-debug.apk


Deterministic Release
---------------------

Having a deterministic, repeatable build process that produces the exact same
APK wherever it is run has a lot of benefits:

* makes it easy for anyone to verify that the official APKs are indeed
  generated only from the sources in git

* makes it possible for FDroid to distribute APKs with the upstream
  developer's signature instead of the FDroid's signature

To increase the likelyhood of producing a deterministic build of Checkey, run
the java build with `faketime`.  The rest is already included in the
Makefiles.  This is also included in the ./make-release-build.sh
script. Running a program with `faketime` causes that program to recent a
fixed time based on the timestamp provided to `faketime`.  This ensures that
the timestamps in the files are always the same.

  faketime "`git log -n1 --format=format:%ai`" \
    ant clean debug

Release Builds
--------------

To reproduce your own version of the official releases, use
`./make-release-build`.  _WARNING_: this will wipe out any changes to the git
repo in the process to ensure that the release build is only built from data
that is committed to git, so it is probably best to run it in a clean clone.
Then you can compare your release build to the official release using the
included `./compare-to-official-release` script.  It requires a few utilities
to work.  All of them are Debian/Ubuntu packages except for `apktool`.  Here's
what to install:

  apt-get install unzip meld bsdmainutils

Or on OSX with brew:

  brew install apktool unzip


Licenses
========

The app itself it released under the GNU GPLv3+.  Here are some specific
credits:

* app icon: Ada Lovelace as a child (public domain)

* app feature graphic: https://commons.wikimedia.org/wiki/File:SZ42-6-wheels-lightened.jpg
