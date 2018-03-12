#!/usr/bin/env bash

set -e

email=${EMAIL:-user@tcbuildreport.com}
key=${KEY:-tcbuildreport.key}

ssh-keygen -t rsa -C "$email" -f $key -N ''
