#
# Copyright (c) 2023, 2025 Oracle and/or its affiliates.
#

#!awk -f
#
# Extracts the <dependencyManagement> section out of a pom file and
# converts it to just <dependencies>.
#
# While doing this it strips out exclusions and versions
BEGIN {
    inDependencies="false"
    inExclusions="false"
}

/<dependencies>/ {
    # only print dependencies
    inDependencies="true"
}

/<\/dependencies>/ {
    inDependencies="false"
    print $0
}

/<exclusions>/ {
    # strip exclusions
    inExclusions="true"
    next
}

/<\/exclusions>/ {
    # strip exclusions
    inExclusions="false"
    next
}

/<version>/ {
    # strip version
    next
}

inDependencies == "true" && inExclusions == "false" {
    print $0
}

END {
}
