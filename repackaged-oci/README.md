# Overview

These are all repackaged OCI library modules.

Many of the internal common libraries have what the Helidon team would label as "bad dependencies". These include:
* Dropwizard dependencies.
* Servlet dependencies.
* Deprecated/legacy library dependencies (e.g., jsr305).
* etc.

The Helidon team is providing repacked libraries here to compensate for this temporarily while we work with these teams to help
fix these libraries at the source.


## Disclaimer

The Helidon team will support these repackaged libraries to the best of our ability. Ultimately, however, the
goal is to eventually remove all of these repackaged libraries and use the original libraries directly. It may
be the case, therefore, that some issues will not be able to be addressed without direct involvement from these
underlying teams providing these libraries.