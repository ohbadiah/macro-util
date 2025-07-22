#!/bin/bash

set -exu

rsync -u nutrition.db nmdc:
rsync -u nmdc:nutrition.db .
