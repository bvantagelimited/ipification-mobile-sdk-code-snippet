# Network routing examples

- [`android/request-scoped`](android/request-scoped/README.md): route only IPification requests
  through cellular. This is the recommended Android integration.
- [`android/process-scoped`](android/process-scoped/README.md): route future connections from
  the Android app process through cellular. Use only when the whole process must be bound.
- [`ios/request-scoped`](ios/request-scoped/README.md): create cellular-only connections with
  Apple's Network framework. iOS has no public whole-process equivalent.

Complete runnable applications are kept separately under [`../samples`](../samples/README.md).
