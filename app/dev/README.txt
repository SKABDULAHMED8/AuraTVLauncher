DEVELOPMENT SIGNING KEY - NOT THE PRODUCTION KEY

aura-dev.keystore signs debug (development) builds only, so test builds can be
installed over each other. The password is public on purpose.

It must never be used for a release, and the release build type must never
reference it. The permanent production key lives only in GitHub Secrets.
