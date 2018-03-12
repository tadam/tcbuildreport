#!/usr/bin/env bash

set -e

email=${EMAIL:-user@tcbuildreport.com}
key=${KEY:-tcbuildreport-dev.key}

ssh-keygen -t rsa -C "$email" -f $key -N ''
