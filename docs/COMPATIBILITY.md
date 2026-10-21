# Compatibility — Universal Captions 3.1.0 test candidate

The experimental `refactor/morphe-native-source` branch produces a **full
replacement Morphe MPP**. Do not stack it with the official Morphe MPP.
Use a clean YouTube APK and one selected source.

The pinned Morphe fork advertises YouTube `21.20.405` and `21.41.525`.
[Candidate CI](https://github.com/itolkien/universal-captions/actions/runs/39544149551)
passed baseline/candidate tests, Android packaging and bundle verification.
One user smoke test reported no visible problems, but the exact tested
YouTube version and complete scenario matrix were not recorded. Neither
advertised version is fully accepted on that evidence alone.

Missing language options are taken from a fixed 156-code Web snapshot, not
Android's native target-language list. Only verified original sources or
consistent native translated templates can be extended; YouTube's backend
determines which targets actually return subtitles.

The current published standalone release is
[Universal Captions 3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1)
on `main`. See the [3.1.0 release checklist](PARITY_VALIDATION.md).
