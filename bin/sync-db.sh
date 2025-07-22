#!/bin/bash

set -xu

rsync -u nutrition.db nmdc:
rsync -u nmdc:nutrition.db .
