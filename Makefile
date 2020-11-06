# Makefile adapted from
# from https://tech.davis-hansson.com/p/make/

# Use bash as the shell
SHELL := bash
# Exit on any shell error
.SHELLFLAGS := -eu -o pipefail -c
# Delete target file if script fails
.DELETE_ON_ERROR:
# Warn when a Make variable is not defined
MAKEFLAGS += --warn-undefined-variables
# Do not use standard rules for C builds
MAKEFLAGS += --no-builtin-rules

# No default target
.PHONY: all
all:


# Working tree state:
ALLOW_DIRTY=false

.PHONY: dirty
dirty:
	$(eval ALLOW_DIRTY=true)
	@echo "WARNING: Deploys will be allowed from a dirty working tree."


# Runs Clojure tests.
# Tests always run in the dev environment
.PHONY: test
test:
	clojure -A:test:runner

# Make sure there aren't uncommitted changes
.PHONY: check-clean-tree
check-clean-tree:
	@if [[ "$(ALLOW_DIRTY)" != "true" && -n "$$(git status --porcelain)" ]]; then \
		echo "ERROR: Working directory not clean."; \
	  exit 97; \
	fi


.PHONY: build
build:
	rm -rf public/js && \
	npm install && \
	shadow-cljs release app

.PHONY: release
release: check-clean-tree build
	# Add the js compilation output and commit
	surge public 2020-election-results.surge.sh

.PHONY: clean
clean:
	rm -rf target/*

