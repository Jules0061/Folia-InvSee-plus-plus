# InvSee++ — Folia Fork (1.21.11)

Fork of [InvSee++](https://github.com/Jannyboy11/InvSee-plus-plus) by Jannyboy11, trimmed and optimized for **Folia / Paper 1.21.11**.

## Changes vs upstream
- `folia-supported: true` — runs natively on Folia (uses `GlobalRegionScheduler`, `AsyncScheduler`, and per-entity `EntityScheduler` instead of the Bukkit scheduler).
- Single platform implementation (`Impl_Paper_1_21_11`) — all 20+ legacy version impls removed. Much smaller jar, faster startup, no dead code shaded in.
- `api-version: 1.21`, plugin module compiled with `--release 21`.
- Authors: Jannyboy11, Jules (Forked it).
- Addon plugins (Give/Clear/Clone), Glowstone and Multiverse-Inventories modules dropped.

## Build
Requires JDK 21 and Maven. One-time NMS init for the Paper module, then package:

```
mvn -N install
mvn install -DskipTests -pl Utils,Mojang_API,FastStats,InvSee++_Common,InvSee++_PerWorldInventory
mvn paper-nms:init -pl InvSee++_Platforms/Impl_Paper_1_21_11
mvn install -DskipTests
```

Output jar: `InvSee++_Plugin/target/InvSee++.jar`

## IntelliJ
Open this folder (or `pom.xml`) as a project — Maven modules import automatically. Project JDK: 21.
