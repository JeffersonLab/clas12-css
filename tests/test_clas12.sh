#!/bin/sh

unzip ../downloads/sns-css-4.1.1-linux.gtk.x86_64.zip
cd sns-css-4.1.1
./css \
    -share_link /usr/clas12/release/dev/epics/css_share=/CLAS12_Share \
    -pluginCustomization ../plugin_customization_clas12.ini \
    -nosplash &

